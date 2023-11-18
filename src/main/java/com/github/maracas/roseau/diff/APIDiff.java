package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class represents Roseau's comparison tool for detecting breaking changes between two API versions.
 */
public class APIDiff {
	/**
	 * The first version of the API to be compared.
	 */
	private final API v1;

	/**
	 * The second version of the API to be compared.
	 */
	private final API v2;

	/**
	 * List of all the breaking changes identified in the comparison.
	 */
	private final List<BreakingChange> breakingChanges;

	/**
	 * Constructs an APIDiff instance to compare two API versions for breaking changes detection.
	 *
	 * @param v1 The first version of the API to compare.
	 * @param v2 The second version of the API to compare.
	 */
	public APIDiff(API v1, API v2) {
		this.v1 = Objects.requireNonNull(v1);
		this.v2 = Objects.requireNonNull(v2);
		this.breakingChanges = new ArrayList<>();
	}
	
	public List<BreakingChange> diff() {
		v1.getExportedTypes().forEach(t1 -> {
			Optional<TypeDecl> findT2 = v2.getExportedType(t1.getQualifiedName());

			findT2.ifPresentOrElse(
				// There is a matching type
				t2 -> {
					diffType(t1, t2);
					diffFields(t1, t2);
					diffMethods(t1, t2);
					diffAddedMethods(t1, t2);

					if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2)
						diffConstructors(c1, c2);
				},
				// Type has been removed
				() -> bc(t1.isClass() ? BreakingChangeKind.CLASS_REMOVED : BreakingChangeKind.INTERFACE_REMOVED, t1)
			);
		});

		return breakingChanges;
	}

	private void diffFields(TypeDecl t1, TypeDecl t2) {
		t1.getFields().forEach(f1 -> {
			Optional<FieldDecl> findF2 = t2.getField(f1.getQualifiedName());

			findF2.ifPresentOrElse(
				// There is a matching field
				f2 -> diffField(f1, f2),
				// The field has been removed
				() -> bc(BreakingChangeKind.FIELD_REMOVED, f1)
			);
		});
	}

	private void diffMethods(TypeDecl t1, TypeDecl t2) {
		t1.getMethods().forEach(m1 -> {
			Optional<MethodDecl> matchM2 = t2.getMethods().stream()
				.filter(m -> m.hasSameSignature(m1))
				.findFirst();

			matchM2.ifPresentOrElse(
				// There is a matching method
				m2 -> diffMethod(t1, m1, m2),
				// The method has been removed
				() -> bc(BreakingChangeKind.METHOD_REMOVED, m1)
			);
		});
	}

	private void diffConstructors(ClassDecl c1, ClassDecl c2) {
		c1.getConstructors().forEach(cons1 -> {
			Optional<ConstructorDecl> matchCons2 = c2.getConstructors().stream()
				.filter(cons -> cons.hasSameSignature(cons1))
				.findFirst();

			matchCons2.ifPresentOrElse(
				// There is a matching constructor
				cons2 -> diffConstructor(cons1, cons2),
				// The constructor has been removed
				() -> bc(BreakingChangeKind.CONSTRUCTOR_REMOVED, cons1)
			);
		});
	}

	private void diffAddedMethods(TypeDecl t1, TypeDecl t2) {
		t2.getMethods().stream()
			.filter(m2 -> t1.getMethods().stream().noneMatch(m1 -> m1.hasSameSignature(m2)))
			.forEach(m2 -> {
				if (t2.isInterface() && !m2.isDefault())
					bc(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, t1);

				if (t2.isClass() && m2.isAbstract())
					bc(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, t1);
			});
	}

	private void diffType(TypeDecl t1, TypeDecl t2) {
		if (t1.isClass()) {
			if (!t1.isFinal() && t2.isFinal())
				bc(BreakingChangeKind.CLASS_NOW_FINAL, t1);

			if (!t1.isAbstract() && t2.isAbstract())
				bc(BreakingChangeKind.CLASS_NOW_ABSTRACT, t1);

			if (!t1.isStatic() && t2.isStatic() && t1.isNested() && t2.isNested())
				bc(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, t1);

			if (t1.isStatic() && !t2.isStatic() && t1.isNested() && t2.isNested())
				bc(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, t1);

			if (!t1.isCheckedException() && t2.isCheckedException())
				bc(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, t1);
		}

		if (t1.isPublic() && t2.isProtected())
			bc(BreakingChangeKind.TYPE_LESS_ACCESSIBLE, t1);

		if (t1 instanceof ClassDecl cls1 && t2 instanceof ClassDecl cls2) {
			if (cls1.getSuperClass() != null && cls2.getSuperClass() == null)
				bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, t1);

			// Check for deleted super-interfaces
			if (t1.getSuperInterfaces().stream()
				.anyMatch(intf1 -> t2.getSuperInterfaces().stream().noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
				bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, t1);
		}

		if (t1.isInterface() && t1.getSuperInterfaces().stream()
			.anyMatch(intf1 -> t2.getSuperInterfaces().stream().noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
			bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, t1);

		if (!t1.getClass().equals(t2.getClass()))
			bc(BreakingChangeKind.CLASS_TYPE_CHANGED, t1);

		int formalParametersCount1 = t1.getFormalTypeParameters().size();
		int formalParametersCount2 = t2.getFormalTypeParameters().size();
		if (formalParametersCount1 == formalParametersCount2) {
			for (int i = 0; i < formalParametersCount1; i++) {
				FormalTypeParameter p1 = t1.getFormalTypeParameters().get(i);
				FormalTypeParameter p2 = t2.getFormalTypeParameters().get(i);

				List<String> bounds1 = p1.bounds().stream()
					.map(TypeReference::getQualifiedName)
					.toList();
				List<String> bounds2 = p2.bounds().stream()
					.map(TypeReference::getQualifiedName)
					.toList();

				if (bounds1.size() != bounds2.size()
					|| !(new HashSet<>(bounds1)).equals(new HashSet<>(bounds2))) {
					bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, t1);
				}
			}
		} else if (formalParametersCount1 < formalParametersCount2) {
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, t1);
		} else {
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, t1);
		}
	}

	private void diffField(FieldDecl f1, FieldDecl f2) {
		if (!f1.isFinal() && f2.isFinal())
			bc(BreakingChangeKind.FIELD_NOW_FINAL, f1);

		if (!f1.isStatic() && f2.isStatic())
			bc(BreakingChangeKind.FIELD_NOW_STATIC, f1);

		if (f1.isStatic() && !f2.isStatic())
			bc(BreakingChangeKind.FIELD_NO_LONGER_STATIC, f1);

		if (!f1.getType().equals(f2.getType()))
			bc(BreakingChangeKind.FIELD_TYPE_CHANGED, f1);

		if (f1.isPublic() && f2.isProtected())
			bc(BreakingChangeKind.FIELD_LESS_ACCESSIBLE, f1);
	}

	private void diffMethod(TypeDecl t1, MethodDecl m1, MethodDecl m2) {
		if (!m1.isFinal() && m2.isFinal())
			bc(BreakingChangeKind.METHOD_NOW_FINAL, m1);

		if (!m1.isStatic() && m2.isStatic())
			bc(BreakingChangeKind.METHOD_NOW_STATIC, m1);

		if (!m1.isNative() && m2.isNative())
			bc(BreakingChangeKind.METHOD_NOW_NATIVE, m1);

		if (m1.isStatic() && !m2.isStatic())
			bc(BreakingChangeKind.METHOD_NO_LONGER_STATIC, m1);

		if (m1.isStrictFp() && !m2.isStrictFp())
			bc(BreakingChangeKind.METHOD_NO_LONGER_STRICTFP, m1);

		if (!m1.isAbstract() && m2.isAbstract())
			bc(BreakingChangeKind.METHOD_NOW_ABSTRACT, m1);

		if (m1.isAbstract() && m2.isDefault()) // Careful
			bc(BreakingChangeKind.METHOD_ABSTRACT_NOW_DEFAULT, m1);

		if (m1.isPublic() && m2.isProtected())
			bc(BreakingChangeKind.METHOD_LESS_ACCESSIBLE, m1);

		if (!m1.getReturnType().equals(m2.getReturnType()))
			bc(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, m1);

		List<TypeReference<ClassDecl>> additionalExceptions1 = m1.getThrownExceptions().stream()
			.filter(TypeReference::isCheckedException)
			.filter(e -> !m2.getThrownExceptions().contains(e))
			.toList();

		List<TypeReference<ClassDecl>> additionalExceptions2 = m2.getThrownExceptions().stream()
			.filter(e -> !m1.getThrownExceptions().contains(e))
			.toList();

		if (!additionalExceptions1.isEmpty())
			bc(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, m1);

		if (!additionalExceptions2.isEmpty())
			bc(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, m1);

		// JLS says only one vararg per method, in last position
		/*IntStream.range(0, method1.getParametersVarargsCheck().size())
			.filter(i -> method1.getParametersVarargsCheck().get(i) != method2.getParametersVarargsCheck().get(i))
			.forEach(i -> {
				boolean isNowVarargs = !method1.getParametersVarargsCheck().get(i) && method2.getParametersVarargsCheck().get(i);
				BreakingChangeKind kind = isNowVarargs ? BreakingChangeKind.METHOD_NOW_VARARGS : BreakingChangeKind.METHOD_NO_LONGER_VARARGS;
				bc(kind, method2.getLocation(), BreakingChangeNature.MUTATION, method2));
			});*/

		// Handling the formal type parameters additions and deletions
		// In classes
		if (t1.isClass()) {
			if (m1.getFormalTypeParameters().size() > m2.getFormalTypeParameters().size() && !m2.getFormalTypeParameters().isEmpty())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, m1);

			if (m1.getFormalTypeParameters().size() < m2.getFormalTypeParameters().size() && !m1.getFormalTypeParameters().isEmpty())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, m1);
		}

		// In interfaces
		if (t1.isInterface()) {
			if (m1.getFormalTypeParameters().size() > m2.getFormalTypeParameters().size())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, m1);

			if (m1.getFormalTypeParameters().size() < m2.getFormalTypeParameters().size() && !m1.getFormalTypeParameters().isEmpty())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, m1);
		}


		// Handling changes in the formal type parameters' bounds
		// The order of the FormalTypeParameters matters but the order of their bounds doesn't, this is why
		// I'm transforming the bounds into hashsets
//		if (method1.getFormalTypeParameters().size() == method2.getFormalTypeParameters().size()) {
//			List<List<String>> boundsV1 = method1.getFormalTypeParamsBounds();
//			List<List<String>> boundsV2 = method2.getFormalTypeParamsBounds();
//
//			for (int i = 0; i < boundsV2.size(); i++) {
//				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
//				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);
//
//				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
//				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);
//
//				// Every bound change is breaking in interfaces, no matter the nature
//				if (type1.getDeclarationType().equals(TypeKind.INTERFACE) && !boundsSetV1.equals(boundsSetV2))
//						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getLocation(), BreakingChangeNature.MUTATION, method2));
//
//				// In classes
//				if (type1.getDeclarationType().equals(TypeKind.CLASS)) {
//					// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
//					if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
//						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getLocation(), BreakingChangeNature.MUTATION, method2));
//
//					// The addition of a bound is breaking
//					if (boundsSetV1.size() < boundsSetV2.size())
//						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, method2.getLocation(), BreakingChangeNature.MUTATION, method2));
//				}
//			}
//		}
	}

	private void diffConstructor(ConstructorDecl cons1, ConstructorDecl cons2) {
		if (cons1.isPublic() && cons2.isProtected())
			bc(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, cons1);

//		if (!constructor1.getParametersReferencedTypes().equals(constructor2.getParametersReferencedTypes()))
//			bc(BreakingChangeKind.CONSTRUCTOR_PARAMS_GENERICS_CHANGED, constructor2.getLocation(), BreakingChangeNature.MUTATION, constructor2));

//		if (constructor1.getFormalTypeParameters().size() == constructor2.getFormalTypeParameters().size()) {
//			List<List<String>> boundsV1 = constructor1.getFormalTypeParamsBounds();
//			List<List<String>> boundsV2 = constructor2.getFormalTypeParamsBounds();
//
//			for (int i = 0; i < boundsV2.size(); i++) {
//				List<String> boundsOfTheFormalTypeParameterV1 = boundsV1.get(i);
//				List<String> boundsOfTheFormalTypeParameterV2 = boundsV2.get(i);
//
//				HashSet<String> boundsSetV1 = new HashSet<>(boundsOfTheFormalTypeParameterV1);
//				HashSet<String> boundsSetV2 = new HashSet<>(boundsOfTheFormalTypeParameterV2);
//
//				// If the sets have equal sizes but are not equal themselves, it means that an element changed within them, which is breaking
//				if (!boundsSetV1.equals(boundsSetV2) && boundsSetV1.size() == boundsSetV2.size())
//					bc(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED, constructor2.getLocation(), BreakingChangeNature.MUTATION, constructor2));
//
//				// The addition of a bound is breaking
//				if (boundsSetV1.size() < boundsSetV2.size())
//					bc(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_CHANGED, constructor2.getLocation(), BreakingChangeNature.MUTATION, constructor2));
//			}
//		}


//		if (constructor1.getFormalTypeParameters().size() > constructor2.getFormalTypeParameters().size() && !constructor2.getFormalTypeParameters().isEmpty())
//			bc(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_REMOVED, constructor2.getLocation(), BreakingChangeNature.DELETION, constructor2));
//
//		if (constructor1.getFormalTypeParameters().size() < constructor2.getFormalTypeParameters().size() && !constructor1.getFormalTypeParameters().isEmpty())
//			bc(BreakingChangeKind.CONSTRUCTOR_FORMAL_TYPE_PARAMETERS_ADDED, constructor2.getLocation(), BreakingChangeNature.ADDITION, constructor2));
	}

	private void bc(BreakingChangeKind kind, Symbol impactedSymbol) {
		breakingChanges.add(new BreakingChange(kind, impactedSymbol));
	}

	/**
	 * Retrieves the list of all the breaking changes detected between the two API versions.
	 *
	 * @return List of all the breaking changes
	 */
	public List<BreakingChange> getBreakingChanges() {
		return breakingChanges;
	}

	/**
	 * Generates a csv report for the detected breaking changes. This report includes the kind, type name,
	 * <p>
	 * position, associated element, and nature of each detected BC.
	 */
	public void breakingChangesReport() {
		List<BreakingChange> bcs = getBreakingChanges();

		try (FileWriter writer = new FileWriter("breaking_changes_report.csv")) {
			writer.write("Kind,Element,Nature,Position\n");

			for (BreakingChange breakingChange : bcs) {
				String kind = breakingChange.kind().toString();
				String element = breakingChange.impactedSymbol().getQualifiedName();
				String nature = breakingChange.kind().getNature().toString();
				SourceLocation location = breakingChange.impactedSymbol().getLocation();

				writer.write(kind + "," + element + "," + nature + "," + location + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a string representation of the breaking changes list.
	 *
	 * @return A formatted string containing all the breaking changes and their info.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (BreakingChange breakingChange : breakingChanges) {
			result.append(breakingChange.toString()).append("\n");
			result.append("    =========================\n\n");
		}

		return result.toString();
	}
}
