package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import com.github.maracas.roseau.diff.changes.BreakingChangeNature;

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
	
	public void bc(BreakingChangeKind kind, SourceLocation position, BreakingChangeNature nature, Symbol impactedSymbol) {
		breakingChanges.add(new BreakingChange(kind, position, nature, impactedSymbol));
	}

	public List<BreakingChange> diff() {
		v1.getExportedTypes().forEach(t1 -> {
			Optional<TypeDecl> t2 = v2.getExportedType(t1.getQualifiedName());

			// Type has been removed
			if (t2.isEmpty()) {
				if (t1.isClass())
					bc(BreakingChangeKind.CLASS_REMOVED, t1.getLocation(), BreakingChangeNature.DELETION, t1);

				if (t1.isInterface())
					bc(BreakingChangeKind.INTERFACE_REMOVED, t1.getLocation(), BreakingChangeNature.DELETION, t1);
			}
			// There is a matching type
			else {
				diffTypes(t1, t2.get());
			}
		});

		return breakingChanges;
	}

	private void diffTypes(TypeDecl type1, TypeDecl type2) {
		if (type1.isClass()) {
			if (!type1.getModifiers().contains(Modifier.FINAL) && type2.getModifiers().contains(Modifier.FINAL))
				bc(BreakingChangeKind.CLASS_NOW_FINAL, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

			if (!type1.getModifiers().contains(Modifier.ABSTRACT) && type2.getModifiers().contains(Modifier.ABSTRACT))
				bc(BreakingChangeKind.CLASS_NOW_ABSTRACT, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

			if (!type1.getModifiers().contains(Modifier.STATIC) && type2.getModifiers().contains(Modifier.STATIC) && type1.isNested() && type2.isNested())
				bc(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

			if (type1.getModifiers().contains(Modifier.STATIC) && !type2.getModifiers().contains(Modifier.STATIC) && type1.isNested() && type2.isNested())
				bc(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

			if (!type1.isCheckedException() && type2.isCheckedException())
				bc(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, type2.getLocation(), BreakingChangeNature.MUTATION, type2);
		}

		if (type1.getVisibility().equals(AccessModifier.PUBLIC) && type2.getVisibility().equals(AccessModifier.PROTECTED))
			bc(BreakingChangeKind.TYPE_LESS_ACCESSIBLE, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

		if (type1 instanceof ClassDecl cls1 && type2 instanceof ClassDecl cls2) {
			if (cls1.getSuperClass() != null && cls2.getSuperClass() == null)
				bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

			// Check for deleted super-interfaces
			if (type1.getSuperInterfaces().stream()
				.anyMatch(intf1 -> type2.getSuperInterfaces().stream().noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
					bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getLocation(), BreakingChangeNature.MUTATION, type2);
		}

		if (type1.isInterface() && type1.getSuperInterfaces().stream()
			.anyMatch(intf1 -> type2.getSuperInterfaces().stream().noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
				bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

		if (!type1.getClass().equals(type2.getClass()))
			bc(BreakingChangeKind.CLASS_TYPE_CHANGED, type2.getLocation(), BreakingChangeNature.MUTATION, type2);

		int formalParametersCount1 = type1.getFormalTypeParameters().size();
		int formalParametersCount2 = type2.getFormalTypeParameters().size();
		if (formalParametersCount1 == formalParametersCount2) {
			for (int i = 0; i < formalParametersCount1; i++) {
				FormalTypeParameter p1 = type1.getFormalTypeParameters().get(i);
				FormalTypeParameter p2 = type2.getFormalTypeParameters().get(i);

				List<String> bounds1 = p1.bounds().stream()
					.map(TypeReference::getQualifiedName)
					.toList();
				List<String> bounds2 = p2.bounds().stream()
					.map(TypeReference::getQualifiedName)
					.toList();

				if (bounds1.size() != bounds2.size()
					|| !(new HashSet<>(bounds1)).equals(new HashSet<>(bounds2))) {
					bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, type2.getLocation(), BreakingChangeNature.MUTATION, type2);
				}
			}
		} else if (formalParametersCount1 < formalParametersCount2) {
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, type2.getLocation(), BreakingChangeNature.DELETION, type2);
		} else {
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, type2.getLocation(), BreakingChangeNature.ADDITION, type2);
		}

		// Diffing fields
		type1.getFields().forEach(f1 -> {
			Optional<FieldDecl> f2 = type2.getField(f1.getQualifiedName());

			// The field has been removed
			if (f2.isEmpty()) {
				bc(BreakingChangeKind.FIELD_REMOVED, f1.getLocation(), BreakingChangeNature.DELETION, f1);
			}
			// There is a matching field
			else {
				diffFields(f1, f2.get());
			}
		});

		// Diffing methods
		type1.getMethods().forEach(m1 -> {
			Optional<MethodDecl> m2 = type2.getMethods().stream()
				.filter(m -> m.hasSameSignature(m1))
				.findFirst();

			// The method has been removed
			if (m2.isEmpty()) {
				bc(BreakingChangeKind.METHOD_REMOVED, m1.getLocation(), BreakingChangeNature.DELETION, m1);
			}
			// There is a matching method
			else {
				diffMethods(type1, type2, m1, m2.get());
			}
		});

		// Checking added methods
		type2.getMethods().stream()
			.filter(method2 -> type1.getMethods().stream()
				.noneMatch(method1 -> method1.hasSameSignature(method2)))
			.forEach(m2 -> {
				if (type2.isInterface() && !m2.isDefault())
					bc(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, m2.getLocation(), BreakingChangeNature.ADDITION, m2);

				if (type2.isClass() && m2.getModifiers().contains(Modifier.ABSTRACT))
					bc(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, m2.getLocation(), BreakingChangeNature.ADDITION, m2);
			});

		// Diffing constructors
		if (type1 instanceof ClassDecl c1 && type2 instanceof ClassDecl c2) {
			c1.getConstructors().forEach(cons1 -> {
				Optional<ConstructorDecl> cons2 = c2.getConstructors().stream()
					.filter(cons -> cons.hasSameSignature(cons1))
					.findFirst();

				// The constructor has been removed
				if (cons2.isEmpty()) {
					bc(BreakingChangeKind.CONSTRUCTOR_REMOVED, cons1.getLocation(), BreakingChangeNature.DELETION, cons1);
				}
				// There is a matching constructor
				else {
					diffConstructors(cons1, cons2.get());
				}
			});
		}
	}

	private void diffFields(FieldDecl field1, FieldDecl field2) {
		if (!field1.getModifiers().contains(Modifier.FINAL) && field2.getModifiers().contains(Modifier.FINAL))
			bc(BreakingChangeKind.FIELD_NOW_FINAL, field2.getLocation(), BreakingChangeNature.MUTATION, field2);

		if (!field1.getModifiers().contains(Modifier.STATIC) && field2.getModifiers().contains(Modifier.STATIC))
			bc(BreakingChangeKind.FIELD_NOW_STATIC, field2.getLocation(), BreakingChangeNature.MUTATION, field2);

		if (field1.getModifiers().contains(Modifier.STATIC) && !field2.getModifiers().contains(Modifier.STATIC))
			bc(BreakingChangeKind.FIELD_NO_LONGER_STATIC, field2.getLocation(), BreakingChangeNature.MUTATION, field2);

		if (!field1.getType().equals(field2.getType()))
			bc(BreakingChangeKind.FIELD_TYPE_CHANGED, field2.getLocation(), BreakingChangeNature.MUTATION, field2);

		if (field1.getVisibility().equals(AccessModifier.PUBLIC) && field2.getVisibility().equals(AccessModifier.PROTECTED))
			bc(BreakingChangeKind.FIELD_LESS_ACCESSIBLE, field2.getLocation(), BreakingChangeNature.MUTATION, field2);
	}

	private void diffMethods(TypeDecl type1, TypeDecl type2, MethodDecl method1, MethodDecl method2) {
		if (!method1.getModifiers().contains(Modifier.FINAL) && method2.getModifiers().contains(Modifier.FINAL))
			bc(BreakingChangeKind.METHOD_NOW_FINAL, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (!method1.getModifiers().contains(Modifier.STATIC) && method2.getModifiers().contains(Modifier.STATIC))
			bc(BreakingChangeKind.METHOD_NOW_STATIC, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (!method1.getModifiers().contains(Modifier.NATIVE) && method2.getModifiers().contains(Modifier.NATIVE))
			bc(BreakingChangeKind.METHOD_NOW_NATIVE, method2.getLocation(), BreakingChangeNature.MUTATION, method2);


		if (method1.getModifiers().contains(Modifier.STATIC) && !method2.getModifiers().contains(Modifier.STATIC))
			bc(BreakingChangeKind.METHOD_NO_LONGER_STATIC, method2.getLocation(), BreakingChangeNature.MUTATION, method2);


		if (method1.getModifiers().contains(Modifier.STRICTFP) && !method2.getModifiers().contains(Modifier.STRICTFP))
			bc(BreakingChangeKind.METHOD_NO_LONGER_STRICTFP, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (!method1.getModifiers().contains(Modifier.ABSTRACT) && method2.getModifiers().contains(Modifier.ABSTRACT))
			bc(BreakingChangeKind.METHOD_NOW_ABSTRACT, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (method1.getModifiers().contains(Modifier.ABSTRACT) && method2.isDefault()) // Careful
			bc(BreakingChangeKind.METHOD_ABSTRACT_NOW_DEFAULT, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (method1.getVisibility().equals(AccessModifier.PUBLIC) && method2.getVisibility().equals(AccessModifier.PROTECTED))
			bc(BreakingChangeKind.METHOD_LESS_ACCESSIBLE, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (!method1.getReturnType().equals(method2.getReturnType()))
			bc(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		List<TypeReference<ClassDecl>> additionalExceptions1 = method1.getThrownExceptions().stream()
			.filter(TypeReference::isCheckedException)
			.filter(e -> !method2.getThrownExceptions().contains(e))
			.toList();

		List<TypeReference<ClassDecl>> additionalExceptions2 = method2.getThrownExceptions().stream()
			.filter(e -> !method1.getThrownExceptions().contains(e))
			.toList();

		if (!additionalExceptions1.isEmpty())
			bc(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

		if (!additionalExceptions2.isEmpty())
			bc(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, method2.getLocation(), BreakingChangeNature.MUTATION, method2);

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
		if (type1.isClass()) {
			if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size() && !method2.getFormalTypeParameters().isEmpty())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getLocation(), BreakingChangeNature.DELETION, method2);

			if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size() && !method1.getFormalTypeParameters().isEmpty())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getLocation(), BreakingChangeNature.ADDITION, method2);
		}

		// In interfaces
		if (type1.isInterface()) {
			if (method1.getFormalTypeParameters().size() > method2.getFormalTypeParameters().size())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, method2.getLocation(), BreakingChangeNature.DELETION, method2);

			if (method1.getFormalTypeParameters().size() < method2.getFormalTypeParameters().size() && !method1.getFormalTypeParameters().isEmpty())
				bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, method2.getLocation(), BreakingChangeNature.ADDITION, method2);
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

	private void diffConstructors(ConstructorDecl constructor1, ConstructorDecl constructor2) {
		if (constructor1.getVisibility().equals(AccessModifier.PUBLIC) && constructor2.getVisibility().equals(AccessModifier.PROTECTED))
			bc(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, constructor2.getLocation(), BreakingChangeNature.MUTATION, constructor2);

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
		List<BreakingChange> breakingChanges = getBreakingChanges();

		try (FileWriter writer = new FileWriter("breaking_changes_report.csv")) {
			writer.write("Kind,Element,Nature,Position\n");

			for (BreakingChange breakingChange : breakingChanges) {
				String kind = breakingChange.kind().toString();
				String element = breakingChange.impactedSymbol().getQualifiedName();
				String nature = breakingChange.nature().toString();
				SourceLocation location = breakingChange.location();

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
