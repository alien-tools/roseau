package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.ExecutableDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.Symbol;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
		v1.getExportedTypes().stream().parallel().forEach(t1 -> {
			v2.findExportedType(t1.getQualifiedName()).ifPresentOrElse(
				// There is a matching type
				t2 -> {
					diffType(t1, t2);
					diffFields(t1, t2);
					diffMethods(t1, t2);
					diffAddedMethods(t1, t2);
				},
				// Type has been removed
				() -> bc(BreakingChangeKind.TYPE_REMOVED, t1)
			);
		});

		return breakingChanges;
	}

	private void diffFields(TypeDecl t1, TypeDecl t2) {
		t1.getFields().forEach(f1 -> {
			t2.findField(f1.getSimpleName()).ifPresentOrElse(
				// There is a matching field
				f2 -> diffField(f1, f2),
				// The field has been removed
				() -> bc(BreakingChangeKind.FIELD_REMOVED, f1)
			);
		});
	}

	private void diffMethods(TypeDecl t1, TypeDecl t2) {
		t1.getMethods().forEach(m1 -> {
			Optional<MethodDecl> matchM2 = t2.getAllMethods().stream()
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
		t2.getAllMethods().stream()
			.filter(m2 -> t1.getAllMethods().stream().noneMatch(m1 -> m1.hasSameSignature(m2)))
			.forEach(m2 -> {
				if (t2.isInterface() && !m2.isDefault())
					bc(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, t1);

				if (t2.isClass() && m2.isAbstract())
					bc(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, t1);
			});
	}

	private void diffType(TypeDecl t1, TypeDecl t2) {
		if (t1.isPublic() && t2.isProtected())
			bc(BreakingChangeKind.TYPE_NOW_PROTECTED, t1);

		if (!t1.getClass().equals(t2.getClass()))
			bc(BreakingChangeKind.CLASS_TYPE_CHANGED, t1);

		// Deleted super-interfaces
		if (t1.getImplementedInterfaces().stream()
				.anyMatch(intf1 ->
					t2.getImplementedInterfaces().stream()
						.noneMatch(intf2 -> intf1.getQualifiedName().equals(intf2.getQualifiedName()))
				))
			bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, t1);

		diffFormalTypeParameters(t1, t2);

		if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2)
			diffClass(c1, c2);
	}

	private void diffClass(ClassDecl c1, ClassDecl c2) {
		if (!c1.isEffectivelyFinal() && c2.isEffectivelyFinal())
			bc(BreakingChangeKind.CLASS_NOW_FINAL, c1);

		if (!c1.isSealed() && c2.isSealed())
			bc(BreakingChangeKind.CLASS_NOW_FINAL, c1);

		if (!c1.isAbstract() && c2.isAbstract())
			bc(BreakingChangeKind.CLASS_NOW_ABSTRACT, c1);

		if (!c1.isStatic() && c2.isStatic() && c1.isNested() && c2.isNested())
			bc(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, c1);

		if (c1.isStatic() && !c2.isStatic() && c1.isNested() && c2.isNested())
			bc(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, c1);

		if (c1.isUncheckedException() && c2.isCheckedException())
			bc(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, c1);

		if (c1.getSuperClass().isPresent() && c2.getSuperClass().isEmpty())
			bc(BreakingChangeKind.SUPERCLASS_MODIFIED_INCOMPATIBLE, c1);

		diffConstructors(c1, c2);
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

		if (m1.isAbstract() && m2.isDefault())
			bc(BreakingChangeKind.METHOD_ABSTRACT_NOW_DEFAULT, m1);

		if (m1.isPublic() && m2.isProtected())
			bc(BreakingChangeKind.METHOD_LESS_ACCESSIBLE, m1);

		if (!Objects.equals(m1.getType(), m2.getType()))
			bc(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, m1);

		diffThrownExceptions(m1, m2);
		diffFormalTypeParameters(m1, m2);
		diffParameters(m1, m2);
	}

	private void diffConstructor(ConstructorDecl cons1, ConstructorDecl cons2) {
		if (cons1.isPublic() && cons2.isProtected())
			bc(BreakingChangeKind.CONSTRUCTOR_LESS_ACCESSIBLE, cons1);

		diffThrownExceptions(cons1, cons2);
		diffFormalTypeParameters(cons1, cons2);
		diffParameters(cons1, cons2);
	}

	private void diffThrownExceptions(ExecutableDecl e1, ExecutableDecl e2) {
		List<TypeReference<ClassDecl>> thrown1 = e1.getThrownCheckedExceptions().stream()
			.filter(exc1 ->
				e2.getThrownCheckedExceptions().stream()
					.noneMatch(exc2 -> exc2.isSubtypeOf(exc1)))
			.toList();

		List<TypeReference<ClassDecl>> thrown2 = e2.getThrownCheckedExceptions().stream()
			.filter(exc2 ->
				e1.getThrownCheckedExceptions().stream()
					.noneMatch(exc1 -> exc2.isSubtypeOf(exc1)))
			.toList();

		if (!thrown1.isEmpty())
			bc(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, e1);

		if (!thrown2.isEmpty())
			bc(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, e1);
	}

	private void diffParameters(ExecutableDecl e1, ExecutableDecl e2) {
		// JLS says only one vararg per method, in last position
		if (!e1.getParameters().isEmpty() && e1.getParameters().getLast().isVarargs()
			&& (e2.getParameters().isEmpty() || !e2.getParameters().getLast().isVarargs()))
			bc(BreakingChangeKind.METHOD_NO_LONGER_VARARGS, e1);

		if (!e2.getParameters().isEmpty() && e2.getParameters().getLast().isVarargs()
			&& (e1.getParameters().isEmpty() || !e1.getParameters().getLast().isVarargs()))
			bc(BreakingChangeKind.METHOD_NOW_VARARGS, e1);
		
		// We checked executable signatures, so we know params are equals modulo type arguments
		for (int i = 0; i < e1.getParameters().size(); i++) {
			ParameterDecl p1 = e1.getParameters().get(i);
			ParameterDecl p2 = e2.getParameters().get(i);

			if (p1.type() instanceof TypeReference<?> t1 && p2.type() instanceof TypeReference<?> t2)
				diffParameterGenerics(e1, t1, t2);
		}
	}

	/**
	 * In general, we need to distinguish how formal type parameters and parameter generics
	 * are handled between methods and constructors: the former can be overridden (thus parameters
	 * are immutable so that signatures in sub/super-classes match) and the latter cannot (thus
	 * parameters can follow variance rules).
	 */
	private void diffParameterGenerics(ExecutableDecl e, TypeReference<?> t1, TypeReference<?> t2) {
		if (t1.getTypeArguments().size() == t2.getTypeArguments().size()) {
			for (int j = 0; j < t1.getTypeArguments().size(); j++) {
				ITypeReference ta1 = t1.getTypeArguments().get(j);
				ITypeReference ta2 = t2.getTypeArguments().get(j);

				if (!ta1.equals(ta2)) { // No match
					if (e instanceof MethodDecl) // Invariant
						bc(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, e);
					else { // Variance
						if (ta2 instanceof WildcardTypeReference wtr && wtr.isUnbounded()) // Unbounded wildcard is always fine
							continue;
						if (ta1 instanceof WildcardTypeReference wtr1 && ta2 instanceof WildcardTypeReference wtr2 && wtr1.upper() == wtr2.upper()) {
							// Changing upper bound to supertype is fine
							if (wtr1.upper() && wtr1.bounds().getFirst().isSubtypeOf(wtr2.bounds().getFirst()))
								continue;
							// Changing lower bound to subtype is fine
							if (!wtr1.upper() && wtr2.bounds().getFirst().isSubtypeOf(wtr1.bounds().getFirst()))
								continue;
						}

						// Other cases break
						bc(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, e);
					}
				}
			}
		} else
			bc(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, e);
	}

	private void diffFormalTypeParameters(TypeDecl t1, TypeDecl t2) {
		int paramsCount1 = t1.getFormalTypeParameters().size();
		int paramsCount2 = t2.getFormalTypeParameters().size();
		if (paramsCount1 == paramsCount2) {
			for (int i = 0; i < paramsCount1; i++) {
				FormalTypeParameter p1 = t1.getFormalTypeParameters().get(i);
				FormalTypeParameter p2 = t2.getFormalTypeParameters().get(i);

				// Removing an existing bound is fine, adding isn't
				if (p1.bounds().size() < p2.bounds().size())
					bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, t1);

				// Each bound in the new version should either pre-exist or be a supertype of an existing one
				if (
					!p2.bounds().stream()
						.allMatch(b2 -> p1.bounds().stream().anyMatch(b1 -> b1.equals(b2) || b1.isSubtypeOf(b2)))
				)
					bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, t1);
			}
		} else if (paramsCount1 > paramsCount2) { // Removing formal type parameters always breaks
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, t1);
		} else if (paramsCount1 > 0) { // Adding formal type parameters only breaks if it's not the first
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, t1);
		}
	}

	private void diffFormalTypeParameters(ExecutableDecl e1, ExecutableDecl e2) {
		// Ok, well. Removing a type parameter is breaking if:
		//  - it's a method (due to @Override)
		//  - it's a constructor and there were more than one
		if (e1.getFormalTypeParameters().size() > e2.getFormalTypeParameters().size()
			&& (e1 instanceof MethodDecl || e1.getFormalTypeParameters().size() > 1))
			bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, e1);

		// Adding a type parameter is only breaking if there was already some
		if (!e1.getFormalTypeParameters().isEmpty() && e1.getFormalTypeParameters().size() < e2.getFormalTypeParameters().size())
			bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, e1);

		for (int i = 0; i < e1.getFormalTypeParameters().size(); i++) {
			List<ITypeReference> bounds1 = e1.getFormalTypeParameters().get(i).bounds();

			if (i < e2.getFormalTypeParameters().size()) {
				List<ITypeReference> bounds2 = e2.getFormalTypeParameters().get(i).bounds();

				if (e1 instanceof MethodDecl) { // Invariant
					if (bounds1.size() != bounds2.size()
						|| !new HashSet<>(bounds1).equals(new HashSet<>(bounds2)))
						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, e1);
				} else { // Variance
					// Any new bound that's not a supertype of an existing bound is breaking
					if (
						bounds2.stream()
							.anyMatch(b2 -> bounds1.stream().noneMatch(b1 -> b1.isSubtypeOf(b2)))
					)
						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, e1);
				}
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
	public void writeReport(Path report) throws IOException {
		try (FileWriter writer = new FileWriter(report.toFile(), StandardCharsets.UTF_8)) {
			writer.write("kind,element,nature,position" + System.lineSeparator());
			writer.write(breakingChanges.stream()
				.map(bc -> "%s,%s,%s,%s%n".formatted(
					bc.kind(),
					bc.impactedSymbol().getQualifiedName(),
					bc.kind().getNature(),
					bc.impactedSymbol().getLocation())
				).collect(Collectors.joining(System.lineSeparator())));
			}
	}

	/**
	 * Generates a string representation of the breaking changes list.
	 *
	 * @return A formatted string containing all the breaking changes and their info.
	 */
	@Override
	public String toString() {
		return breakingChanges.stream()
			.map(BreakingChange::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
