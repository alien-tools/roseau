package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.changes.NonBreakingChange;
import io.github.alien.roseau.diff.changes.NonBreakingChangeKind;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Computes the list of breaking changes between two {@link LibraryTypes} instances.
 * <br>
 * The compared APIs are visited deeply to match their symbols pairwise based on their unique name, and compare their
 * properties when their names match. This implementation visits all {@link TypeDecl} instances in parallel.
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
	private final Set<BreakingChange> breakingChanges;
	private final Set<NonBreakingChange> nonBreakingChanges;

	/**
	 * Constructs an APIDiff instance to compare two {@link LibraryTypes} versions for breaking changes detection.
	 *
	 * @param v1 The first version of the API to compare.
	 * @param v2 The second version of the API to compare.
	 */
	public APIDiff(API v1, API v2) {
		this.v1 = Objects.requireNonNull(v1);
		this.v2 = Objects.requireNonNull(v2);
		breakingChanges = ConcurrentHashMap.newKeySet();
		nonBreakingChanges = ConcurrentHashMap.newKeySet();
	}

	/**
	 * Diff the two APIs to detect breaking changes.
	 *
	 * @return the list of all identified {@link BreakingChange}
	 */
	public List<BreakingChange> diff() {
		v1.getExportedTypes().stream().parallel().forEach(t1 ->
			v2.findExportedType(t1.getQualifiedName()).ifPresentOrElse(
				// There is a matching type
				t2 -> diffType(t1, t2),
				// Type has been removed
				() -> bc(BreakingChangeKind.TYPE_REMOVED, t1, null)
			)
		);

		// Record non-breaking added types
		v2.getExportedTypes().stream().parallel()
			.filter(t2 -> v1.findExportedType(t2.getQualifiedName()).isEmpty())
			.forEach(t2 -> nbc(NonBreakingChangeKind.TYPE_ADDED, null, t2));

		return getBreakingChanges();
	}

	private void diffFields(TypeDecl t1, TypeDecl t2) {
		v1.getAllFields(t1).forEach(f1 ->
			v2.findField(t2, f1.getSimpleName()).ifPresentOrElse(
				// There is a matching field
				f2 -> diffField(f1, f2),
				// The field has been removed
				() -> bc(BreakingChangeKind.FIELD_REMOVED, f1, null)
			)
		);

		// Added fields (non-breaking)
		v2.getAllFields(t2).forEach(f2 -> {
			boolean exists = v1.getAllFields(t1).stream().anyMatch(f1 -> f1.getSimpleName().equals(f2.getSimpleName()));
			if (!exists) {
				nbc(NonBreakingChangeKind.FIELD_ADDED, null, f2);
			}
		});
	}

	private void diffMethods(TypeDecl t1, TypeDecl t2) {
		v1.getAllMethods(t1).forEach(m1 ->
			v2.findMethod(t2, v2.getErasure(m1)).ifPresentOrElse(
				// There is a matching method
				m2 -> diffMethod(m1, m2),
				// The method has been removed
				() -> bc(BreakingChangeKind.METHOD_REMOVED, m1, null)
			)
		);

		// Added methods (non-breaking)
		v2.getAllMethods(t2).forEach(m2 -> {
			boolean exists = v1.getAllMethods(t1).stream().anyMatch(m1 -> v1.haveSameErasure(m1, m2));
			if (!exists && !m2.isAbstract()) {
				nbc(NonBreakingChangeKind.METHOD_ADDED, null, m2);
			}
		});
	}

	private void diffConstructors(ClassDecl c1, ClassDecl c2) {
		c1.getDeclaredConstructors().forEach(cons1 ->
			v2.findConstructor(c2, v2.getErasure(cons1)).ifPresentOrElse(
				// There is a matching constructor
				cons2 -> diffConstructor(cons1, cons2),
				// The constructor has been removed
				() -> bc(BreakingChangeKind.CONSTRUCTOR_REMOVED, cons1, null)
			)
		);

		// Added constructors (non-breaking)
		c2.getDeclaredConstructors().forEach(cons2 -> {
			boolean exists = c1.getDeclaredConstructors().stream().anyMatch(cons1 -> v1.haveSameErasure(cons1, cons2));
			if (!exists) {
				nbc(NonBreakingChangeKind.CONSTRUCTOR_ADDED, null, cons2);
			}
		});
	}

	private void diffAddedMethods(TypeDecl t1, TypeDecl t2) {
		v2.getAllMethods(t2).stream()
			.filter(MethodDecl::isAbstract)
			.filter(m2 -> v1.getAllMethods(t1).stream().noneMatch(m1 -> v1.haveSameErasure(m1, m2)))
			.forEach(m2 -> {
				if (t1.isInterface()) {
					bc(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, t1, m2);
				}

				if (t1.isClass()) {
					bc(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, t1, m2);
				}
			});
	}

	private void diffType(TypeDecl t1, TypeDecl t2) {
		if (t1.isPublic() && t2.isProtected()) {
			bc(BreakingChangeKind.TYPE_NOW_PROTECTED, t1, t2);
		}

		if (!t1.getClass().equals(t2.getClass())) {
			bc(BreakingChangeKind.CLASS_TYPE_CHANGED, t1, t2);
		}

		// If a supertype that was exported has been removed,
		// it may have been used in client code for casts
		if (v1.getAllSuperTypes(t1).stream().anyMatch(sup -> v1.isExported(sup) && !v2.isSubtypeOf(t2, sup))) {
			bc(BreakingChangeKind.SUPERTYPE_REMOVED, t1, t2);
		}

		diffFields(t1, t2);
		diffMethods(t1, t2);
		diffAddedMethods(t1, t2);
		diffFormalTypeParameters(t1, t2);

		if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2) {
			diffClass(c1, c2);
		}
	}

	private void diffClass(ClassDecl c1, ClassDecl c2) {
		if (!v1.isEffectivelyFinal(c1) && v2.isEffectivelyFinal(c2)) {
			bc(BreakingChangeKind.CLASS_NOW_FINAL, c1, c2);
		}

		if (!c1.isEffectivelyAbstract() && c2.isEffectivelyAbstract()) {
			bc(BreakingChangeKind.CLASS_NOW_ABSTRACT, c1, c2);
		}

		if (!c1.isStatic() && c2.isStatic() && c1.isNested() && c2.isNested()) {
			bc(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, c1, c2);
		}

		if (c1.isStatic() && !c2.isStatic() && c1.isNested() && c2.isNested()) {
			bc(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, c1, c2);
		}

		if (v1.isUncheckedException(c1) && v2.isCheckedException(c2)) {
			bc(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, c1, c2);
		}

		diffConstructors(c1, c2);

		// Visibility increases (non-breaking)
		if (c1.isProtected() && c2.isPublic()) {
			nbc(NonBreakingChangeKind.TYPE_VISIBILITY_INCREASED, c1, c2);
		}
	}

	private void diffField(FieldDecl f1, FieldDecl f2) {
		if (!f1.isFinal() && f2.isFinal()) {
			bc(BreakingChangeKind.FIELD_NOW_FINAL, f1, f2);
		}

		if (!f1.isStatic() && f2.isStatic()) {
			bc(BreakingChangeKind.FIELD_NOW_STATIC, f1, f2);
		}

		if (f1.isStatic() && !f2.isStatic()) {
			bc(BreakingChangeKind.FIELD_NO_LONGER_STATIC, f1, f2);
		}

		if (!f1.getType().equals(f2.getType())) {
			bc(BreakingChangeKind.FIELD_TYPE_CHANGED, f1, f2);
		}

		if (f1.isPublic() && f2.isProtected()) {
			bc(BreakingChangeKind.FIELD_NOW_PROTECTED, f1, f2);
		}

		if (f1.isProtected() && f2.isPublic()) {
			nbc(NonBreakingChangeKind.FIELD_VISIBILITY_INCREASED, f1, f2);
		}
	}

	private void diffMethod(MethodDecl m1, MethodDecl m2) {
		if (!v1.isEffectivelyFinal(m1) && v2.isEffectivelyFinal(m2)) {
			bc(BreakingChangeKind.METHOD_NOW_FINAL, m1, m2);
		}

		if (!m1.isStatic() && m2.isStatic()) {
			bc(BreakingChangeKind.METHOD_NOW_STATIC, m1, m2);
		}

		if (m1.isStatic() && !m2.isStatic()) {
			bc(BreakingChangeKind.METHOD_NO_LONGER_STATIC, m1, m2);
		}

		if (!m1.isAbstract() && m2.isAbstract()) {
			bc(BreakingChangeKind.METHOD_NOW_ABSTRACT, m1, m2);
		}

		if (m1.isPublic() && m2.isProtected()) {
			bc(BreakingChangeKind.METHOD_NOW_PROTECTED, m1, m2);
		}

		if (m1.isProtected() && m2.isPublic()) {
			nbc(NonBreakingChangeKind.METHOD_VISIBILITY_INCREASED, m1, m2);
		}

		if (!m1.getType().equals(m2.getType())) {
			bc(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, m1, m2);
		}

		diffThrownExceptions(m1, m2);
		diffFormalTypeParameters(m1, m2);
		diffParameters(m1, m2);
	}

	private void diffConstructor(ConstructorDecl cons1, ConstructorDecl cons2) {
		if (cons1.isPublic() && cons2.isProtected()) {
			bc(BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, cons1, cons2);
		}

		if (cons1.isProtected() && cons2.isPublic()) {
			nbc(NonBreakingChangeKind.CONSTRUCTOR_VISIBILITY_INCREASED, cons1, cons2);
		}

		// We report that as a CONSTRUCTOR_REMOVED
		//if (cons1.isVarargs() && !cons2.isVarargs())
		//	bc(BreakingChangeKind.METHOD_NO_LONGER_VARARGS, cons1, cons2);

		diffThrownExceptions(cons1, cons2);
		diffFormalTypeParameters(cons1, cons2);
		diffParameters(cons1, cons2);
	}

	private void diffThrownExceptions(ExecutableDecl e1, ExecutableDecl e2) {
		if (v1.getThrownCheckedExceptions(e1)
			.stream()
			.anyMatch(exc1 -> v2.getThrownCheckedExceptions(e2).stream()
				.noneMatch(exc2 -> v2.isSubtypeOf(exc2, exc1)))) {
			bc(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, e1, e2);
		}

		if (v2.getThrownCheckedExceptions(e2)
			.stream()
			.anyMatch(exc2 -> v1.getThrownCheckedExceptions(e1).stream()
				.noneMatch(exc1 -> v2.isSubtypeOf(exc2, exc1)))) {
			bc(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, e1, e2);
		}
	}

	private void diffParameters(ExecutableDecl e1, ExecutableDecl e2) {
		// We report that as a METHOD_REMOVED
		//if (e1.isVarargs() && !e2.isVarargs())
		//	bc(BreakingChangeKind.METHOD_NO_LONGER_VARARGS, e1, e2);

		// We checked executable erasures, so we know params are equals modulo type arguments
		for (int i = 0; i < e1.getParameters().size(); i++) {
			ParameterDecl p1 = e1.getParameters().get(i);
			ParameterDecl p2 = e2.getParameters().get(i);

			if (p1.type() instanceof TypeReference<?> t1 && p2.type() instanceof TypeReference<?> t2) {
				diffParameterGenerics(e1, e2, t1, t2);
			}
		}
	}

	/**
	 * In general, we need to distinguish how formal type parameters and parameter generics are handled between methods
	 * and constructors: the former can be overridden (thus parameters are immutable so that signatures in
	 * sub/super-classes match) and the latter cannot (thus parameters can follow variance rules).
	 */
	private void diffParameterGenerics(ExecutableDecl e1, ExecutableDecl e2, TypeReference<?> t1, TypeReference<?> t2) {
		if (t1.typeArguments().size() != t2.typeArguments().size()) {
			bc(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, e1, e2);
			return;
		}

		// Should be invariant, but they're not equal
		if (e1.isMethod() && !t1.equals(t2)) {
			bc(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, e1, e2);
		}

		if (e1.isConstructor() && !v1.isSubtypeOf(t1, t2)) {
			bc(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, e1, e2);
		}
	}

	private void diffFormalTypeParameters(TypeDecl t1, TypeDecl t2) {
		int paramsCount1 = t1.getFormalTypeParameters().size();
		int paramsCount2 = t2.getFormalTypeParameters().size();

		// Removing formal type parameters always breaks
		if (paramsCount1 > paramsCount2) {
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, t1, t2);
			return;
		}

		// Adding formal type parameters breaks unless it's the first
		if (paramsCount2 > paramsCount1 && paramsCount1 > 0) {
			bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, t1, t2);
			return;
		}

		for (int i = 0; i < paramsCount1; i++) {
			FormalTypeParameter p1 = t1.getFormalTypeParameters().get(i);
			FormalTypeParameter p2 = t2.getFormalTypeParameters().get(i);

			// Each bound in the new version should be a supertype of an existing one (or the same)
			// so that the type constraints imposed by p1 are stricter than those imposed by p2
			if (p2.bounds().stream()
				.anyMatch(b2 -> !b2.equals(TypeReference.OBJECT) &&
					p1.bounds().stream().noneMatch(b1 -> v2.isSubtypeOf(b1, b2)))) {
				bc(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, t1, t2);
			}
		}
	}

	private void diffFormalTypeParameters(ExecutableDecl e1, ExecutableDecl e2) {
		int paramsCount1 = e1.getFormalTypeParameters().size();
		int paramsCount2 = e2.getFormalTypeParameters().size();

		// Ok, well. Removing a type parameter is breaking if:
		//  - it's a method (due to @Override)
		//  - it's a constructor and there were more than one
		if (paramsCount1 > paramsCount2 && (e1.isMethod() || paramsCount1 > 1)) {
			bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, e1, e2);
		}

		// Adding a type parameter is only breaking if there was already some
		if (paramsCount1 > 0 && paramsCount1 < paramsCount2) {
			bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, e1, e2);
		}

		for (int i = 0; i < paramsCount1; i++) {
			List<ITypeReference> bounds1 = e1.getFormalTypeParameters().get(i).bounds();

			if (i < paramsCount2) {
				List<ITypeReference> bounds2 = e2.getFormalTypeParameters().get(i).bounds();

				if (e1.isMethod()) { // Invariant
					if (!new HashSet<>(bounds1).equals(new HashSet<>(bounds2))) {
						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, e1, e2);
					}
				} else { // Variance
					// Any new bound that's not a supertype of an existing bound is breaking
					if (bounds2.stream()
						// We can safely ignore this bound
						.filter(b2 -> !b2.equals(TypeReference.OBJECT))
						.anyMatch(b2 -> bounds1.stream().noneMatch(b1 -> v1.isSubtypeOf(b1, b2)))) {
						bc(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, e1, e2);
					}
				}
			}
		}
	}

	private void bc(BreakingChangeKind kind, Symbol impactedSymbol, Symbol newSymbol) {
		BreakingChange bc = new BreakingChange(kind, impactedSymbol, newSymbol);
		breakingChanges.add(bc);
	}

	private void nbc(NonBreakingChangeKind kind, Symbol impacted, Symbol added) {
		nonBreakingChanges.add(new NonBreakingChange(kind, impacted, added));
	}

	public List<BreakingChange> getBreakingChanges() {
		return breakingChanges.stream().toList();
	}

	public List<NonBreakingChange> getNonBreakingChanges() {
		return nonBreakingChanges.stream().toList();
	}

	@Override
	public String toString() {
		return breakingChanges.stream()
			.map(BreakingChange::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
