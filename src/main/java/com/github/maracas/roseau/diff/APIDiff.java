package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.ExecutableDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
		breakingChanges = new ArrayList<>();
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
				() -> bc(BreakingChangeKind.TYPE_REMOVED, t1)
			);
		});

		return breakingChanges;
	}

	private void diffFields(TypeDecl t1, TypeDecl t2) {
		t1.getFields().forEach(f1 -> {
			Optional<FieldDecl> findF2 = t2.findField(f1.getSimpleName());

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

			if (!t1.isSealed() && t2.isSealed())
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
			bc(BreakingChangeKind.TYPE_NOW_PROTECTED, t1);

		if (t1 instanceof ClassDecl cls1 && t2 instanceof ClassDecl cls2) {
			if (cls1.getSuperClass().isPresent() && cls2.getSuperClass().isEmpty())
				bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, t1);
		}

		// Deleted super-interfaces
		if (t1.getImplementedInterfaces().stream()
			.anyMatch(intf1 -> t2.getImplementedInterfaces().stream()
				.noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))))
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
					.map(ITypeReference::getQualifiedName)
					.toList();
				List<String> bounds2 = p2.bounds().stream()
					.map(ITypeReference::getQualifiedName)
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

		if (!m1.getType().equals(m2.getType()))
			bc(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, m1);

		List<TypeReference<ClassDecl>> additionalExceptions1 = m1.getThrownExceptions().stream()
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
		if (!m1.getParameters().isEmpty() && m1.getParameters().getLast().isVarargs()
			&& (m2.getParameters().isEmpty() || !m2.getParameters().getLast().isVarargs()))
			bc(BreakingChangeKind.METHOD_NO_LONGER_VARARGS, m1);

		if (!m2.getParameters().isEmpty() && m2.getParameters().getLast().isVarargs()
			&& (m1.getParameters().isEmpty() || !m1.getParameters().getLast().isVarargs()))
			bc(BreakingChangeKind.METHOD_NOW_VARARGS, m1);

		// FIXME: no checks for parameters???

		diffFormalTypeParameters(m1, m2);
	}

	private void diffConstructor(ConstructorDecl cons1, ConstructorDecl cons2) {
		if (cons1.isPublic() && cons2.isProtected())
			bc(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, cons1);

		diffFormalTypeParameters(cons1, cons2);
	}

	private void diffFormalTypeParameters(ExecutableDecl e1, ExecutableDecl e2) {
		if (e1.getFormalTypeParameters().size() > e2.getFormalTypeParameters().size())
			bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, e1);

		if (e1.getFormalTypeParameters().size() < e2.getFormalTypeParameters().size())
			bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, e1);

		for (int i = 0; i < e1.getFormalTypeParameters().size(); i++) {
			List<ITypeReference> bounds1 = e1.getFormalTypeParameters().get(i).bounds();

			if (i < e2.getFormalTypeParameters().size()) {
				List<ITypeReference> bounds2 = e2.getFormalTypeParameters().get(i).bounds();

				if (bounds1.size() != bounds2.size()
					|| !new HashSet<>(bounds1).equals(new HashSet<>(bounds2)))
					bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, e1);
			}
		}
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
	 * Generates a csv report for the detected breaking changes. This report includes the kind, type qualifiedName,
	 * <p>
	 * position, associated element, and nature of each detected BC.
	 */
	public void breakingChangesReport() throws IOException {
		try (FileWriter writer = new FileWriter("breaking_changes_report.csv")) {
			writer.write("Kind,Element,Nature,Position\n");

			for (BreakingChange breakingChange : breakingChanges) {
				String kind = breakingChange.kind().toString();
				String element = breakingChange.impactedSymbol().getQualifiedName();
				String nature = breakingChange.kind().getNature().toString();
				SourceLocation location = breakingChange.impactedSymbol().getLocation();

				writer.write(kind + "," + element + "," + nature + "," + location + "\n");
			}
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
