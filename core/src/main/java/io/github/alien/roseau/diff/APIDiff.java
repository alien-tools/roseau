package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
	}

	/**
	 * Diff the two APIs to detect breaking changes.
	 *
	 * @return the apiReport built for the two APIs
	 */
	public RoseauReport diff() {
		v1.getExportedTypes().stream().parallel().forEach(t1 ->
			v2.findExportedType(t1.getQualifiedName()).ifPresentOrElse(
				// There is a matching type
				t2 -> diffType(t1, t2),
				// Type has been removed
				() -> typeBC(BreakingChangeKind.TYPE_REMOVED, t1, null)
			)
		);

		return new RoseauReport(v1, v2, getBreakingChanges());
	}

	private void diffFields(TypeDecl t1, TypeDecl t2) {
		v1.getAllFields(t1).forEach(f1 ->
			v2.findField(t2, f1.getSimpleName()).ifPresentOrElse(
				// There is a matching field
				f2 -> diffField(t1, f1, f2),
				// The field has been removed
				() -> memberBC(BreakingChangeKind.FIELD_REMOVED, t1, f1, null)
			)
		);
	}

	private void diffMethods(TypeDecl t1, TypeDecl t2) {
		v1.getAllMethods(t1).forEach(m1 ->
			v2.findMethod(t2, v2.getErasure(m1)).ifPresentOrElse(
				// There is a matching method
				m2 -> diffMethod(t1, t2, m1, m2),
				// The method has been removed
				() -> memberBC(BreakingChangeKind.METHOD_REMOVED, t1, m1, null)
			)
		);
	}

	private void diffConstructors(ClassDecl c1, ClassDecl c2) {
		c1.getDeclaredConstructors().forEach(cons1 ->
			v2.findConstructor(c2, v2.getErasure(cons1)).ifPresentOrElse(
				// There is a matching constructor
				cons2 -> diffConstructor(c1, cons1, cons2),
				// The constructor has been removed
				() -> memberBC(BreakingChangeKind.CONSTRUCTOR_REMOVED, c1, cons1, null)
			)
		);
	}

	private void diffAddedMethods(TypeDecl t1, TypeDecl t2) {
		v2.getAllMethods(t2).stream()
			.filter(MethodDecl::isAbstract)
			.filter(m2 -> v1.getAllMethods(t1).stream().noneMatch(m1 -> v1.haveSameErasure(m1, m2)))
			.forEach(m2 -> {
				if (t1.isInterface()) {
					newTypeMemberBC(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, t1, m2);
				}

				if (t1.isClass()) {
					newTypeMemberBC(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, t1, m2);
				}
			});
	}

	private void diffType(TypeDecl t1, TypeDecl t2) {
		if (t1.isPublic() && t2.isProtected()) {
			typeBC(BreakingChangeKind.TYPE_NOW_PROTECTED, t1, t2);
		}

		if (!t1.getClass().equals(t2.getClass())) {
			typeBC(BreakingChangeKind.CLASS_TYPE_CHANGED, t1, t2);
			return; // Avoid all cascading changes
		}

		// If a supertype that was exported has been removed,
		// it may have been used in client code for casts
		if (v1.getAllSuperTypes(t1).stream().anyMatch(sup -> v1.isExported(sup) && !v2.isSubtypeOf(t2, sup))) {
			typeBC(BreakingChangeKind.SUPERTYPE_REMOVED, t1, t2);
		}

		diffFields(t1, t2);
		diffMethods(t1, t2);
		diffAddedMethods(t1, t2);
		diffFormalTypeParameters(t1, t2);

		if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2) {
			diffClass(c1, c2);
		}

		if (t1 instanceof AnnotationDecl a1 && t2 instanceof AnnotationDecl a2) {
			diffAnnotationInterface(a1, a2);
		}
	}

	private void diffClass(ClassDecl c1, ClassDecl c2) {
		if (!v1.isEffectivelyFinal(c1) && v2.isEffectivelyFinal(c2)) {
			typeBC(BreakingChangeKind.CLASS_NOW_FINAL, c1, c2);
		}

		if (!c1.isEffectivelyAbstract() && c2.isEffectivelyAbstract()) {
			typeBC(BreakingChangeKind.CLASS_NOW_ABSTRACT, c1, c2);
		}

		if (!c1.isStatic() && c2.isStatic() && c1.isNested() && c2.isNested()) {
			typeBC(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, c1, c2);
		}

		if (c1.isStatic() && !c2.isStatic() && c1.isNested() && c2.isNested()) {
			typeBC(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, c1, c2);
		}

		if (v1.isUncheckedException(c1) && v2.isCheckedException(c2)) {
			typeBC(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, c1, c2);
		}

		diffConstructors(c1, c2);
	}

	private void diffAnnotationInterface(AnnotationDecl a1, AnnotationDecl a2) {
		if (!a2.getTargets().containsAll(a1.getTargets())) {
			typeBC(BreakingChangeKind.ANNOTATION_TARGET_REMOVED, a1, a2);
		}

		if (a1.isRepeatable() && !a2.isRepeatable()) {
			typeBC(BreakingChangeKind.ANNOTATION_NO_LONGER_REPEATABLE, a1, a2);
		}

		a1.getAnnotationMethods().forEach(m1 -> {
			// Annotation methods do not have parameters, so no overloading going on
			//   -> simple name matching should be enough
			Optional<AnnotationMethodDecl> optMatch = a2.getAnnotationMethods().stream()
				.filter(m2 -> m1.getSimpleName().equals(m2.getSimpleName()))
				.findFirst();

			optMatch.ifPresentOrElse(m2 -> {
				if (m1.hasDefault() && !m2.hasDefault()) {
					memberBC(BreakingChangeKind.ANNOTATION_METHOD_NO_LONGER_DEFAULT, a1, m1, m2);
				}

				if (!m1.getType().equals(m2.getType())) {
					memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, a1, m1, m2);
				}
			}, () -> memberBC(BreakingChangeKind.METHOD_REMOVED, a1, m1, null));
		});

		a2.getAnnotationMethods().stream()
			.filter(m2 -> !m2.hasDefault())
			.filter(m2 -> a1.getAnnotationMethods().stream().noneMatch(m1 -> m1.getSimpleName().equals(m2.getSimpleName())))
			.forEach(m2 -> newTypeMemberBC(BreakingChangeKind.ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT, a1, m2));
	}

	private void diffField(TypeDecl t1, FieldDecl f1, FieldDecl f2) {
		if (!f1.isFinal() && f2.isFinal()) {
			memberBC(BreakingChangeKind.FIELD_NOW_FINAL, t1, f1, f2);
		}

		if (!f1.isStatic() && f2.isStatic()) {
			memberBC(BreakingChangeKind.FIELD_NOW_STATIC, t1, f1, f2);
		}

		if (f1.isStatic() && !f2.isStatic()) {
			memberBC(BreakingChangeKind.FIELD_NO_LONGER_STATIC, t1, f1, f2);
		}

		if (!f1.getType().equals(f2.getType())) {
			memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED, t1, f1, f2);
		}

		if (f1.isPublic() && f2.isProtected()) {
			memberBC(BreakingChangeKind.FIELD_NOW_PROTECTED, t1, f1, f2);
		}
	}

	private void diffMethod(TypeDecl t1, TypeDecl t2, MethodDecl m1, MethodDecl m2) {
		if (!v1.isEffectivelyFinal(t1, m1) && v2.isEffectivelyFinal(t2, m2)) {
			memberBC(BreakingChangeKind.METHOD_NOW_FINAL, t1, m1, m2);
		}

		if (!m1.isStatic() && m2.isStatic()) {
			memberBC(BreakingChangeKind.METHOD_NOW_STATIC, t1, m1, m2);
		}

		if (m1.isStatic() && !m2.isStatic()) {
			memberBC(BreakingChangeKind.METHOD_NO_LONGER_STATIC, t1, m1, m2);
		}

		if (!m1.isAbstract() && m2.isAbstract()) {
			memberBC(BreakingChangeKind.METHOD_NOW_ABSTRACT, t1, m1, m2);
		}

		if (m1.isPublic() && m2.isProtected()) {
			memberBC(BreakingChangeKind.METHOD_NOW_PROTECTED, t1, m1, m2);
		}

		if (!m1.getType().equals(m2.getType())) {
			memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, t1, m1, m2);
		}

		diffThrownExceptions(t1, m1, m2);
		diffFormalTypeParameters(t1, m1, m2);
		diffParameters(t1, m1, m2);
	}

	private void diffConstructor(TypeDecl t1, ConstructorDecl cons1, ConstructorDecl cons2) {
		if (cons1.isPublic() && cons2.isProtected()) {
			memberBC(BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, t1, cons1, cons2);
		}

		diffThrownExceptions(t1, cons1, cons2);
		diffFormalTypeParameters(t1, cons1, cons2);
		diffParameters(t1, cons1, cons2);
	}

	/**
	 * Always binary-compatible.
	 * <ul>
	 *   <li>Throwing a new checked exception breaks invokers only</li>
	 *   <li>No longer throwing a checked exception breaks invokers and overriders</li>
	 *   <li>Throwing a subtype of an existing checked exception (less) breaks overriders</li>
	 *   <li>Throwing a supertype of an existing checked exception (more) breaks invokers</li>
	 *   <li>The only safe case is replacing with a subtype exception when the executable is final</li>
	 * </ul>
	 */
	private void diffThrownExceptions(TypeDecl t1, ExecutableDecl e1, ExecutableDecl e2) {
		List<ITypeReference> thrown1 = v1.getThrownCheckedExceptions(e1);
		List<ITypeReference> thrown2 = v2.getThrownCheckedExceptions(e2);

		thrown1.stream()
			.filter(exc1 -> thrown2.stream().noneMatch(exc2 ->
				// FIXME: correct, but meh
				v2.isSubtypeOf(exc1, exc2) || v1.isEffectivelyFinal(t1, e1)
			))
			.forEach(exc1 ->
				memberBC(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, t1, e1, e2));

		thrown2.stream()
			.filter(exc2 -> thrown1.stream().noneMatch(exc1 -> v2.isSubtypeOf(exc2, exc1)))
			.forEach(exc1 ->
				memberBC(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, t1, e1, e2));
	}

	private void diffParameters(TypeDecl t1, ExecutableDecl e1, ExecutableDecl e2) {
		// We checked executable erasures, so we know parameter types are equals modulo type arguments
		for (int i = 0; i < e1.getParameters().size(); i++) {
			ParameterDecl p1 = e1.getParameters().get(i);
			ParameterDecl p2 = e2.getParameters().get(i);

			if (p1.type() instanceof TypeReference<?> pt1 && p2.type() instanceof TypeReference<?> pt2) {
				diffParameterGenerics(t1, e1, e2, pt1, pt2);
			}
		}
	}

	/**
	 * In general, we need to distinguish how formal type parameters and parameter generics are handled between methods
	 * and constructors: the former can be overridden (thus parameters are immutable so that signatures in
	 * sub/super-classes match) and the latter cannot (thus parameters can follow variance rules).
	 */
	private void diffParameterGenerics(TypeDecl t1, ExecutableDecl e1, ExecutableDecl e2,
	                                   TypeReference<?> pt1, TypeReference<?> pt2) {
		if (pt1.typeArguments().size() != pt2.typeArguments().size()) {
			memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, t1, e1, e2);
			return;
		}

		boolean finalExecutable = v1.isEffectivelyFinal(t1, e1);
		// Can be overridden = invariant
		if (!finalExecutable && !pt1.equals(pt2)) {
			memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, t1, e1, e2);
		}

		// Can't = variance
		if (finalExecutable && !v1.isSubtypeOf(pt1, pt2)) {
			memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, t1, e1, e2);
		}
	}

	private void diffFormalTypeParameters(TypeDecl t1, TypeDecl t2) {
		int paramsCount1 = t1.getFormalTypeParameters().size();
		int paramsCount2 = t2.getFormalTypeParameters().size();

		// Removing formal type parameters always breaks
		if (paramsCount1 > paramsCount2) {
			typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, t1, t2);
			return;
		}

		// Adding formal type parameters breaks unless it's the first
		if (paramsCount2 > paramsCount1 && paramsCount1 > 0) {
			typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, t1, t2);
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
				typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, t1, t2);
			}
		}
	}

	private void diffFormalTypeParameters(TypeDecl t1, ExecutableDecl e1, ExecutableDecl e2) {
		int paramsCount1 = e1.getFormalTypeParameters().size();
		int paramsCount2 = e2.getFormalTypeParameters().size();
		boolean finalExecutable = v1.isEffectivelyFinal(t1, e1);

		// Ok, well. Removing a type parameter is breaking if:
		//  - it's a method (due to @Override)
		//  - it's a constructor and there was more than one
		if (paramsCount1 > paramsCount2 && (!finalExecutable || paramsCount1 > 1)) {
			memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, t1, e1, e2);
		}

		// Adding a type parameter is only breaking if there was already some
		if (paramsCount1 > 0 && paramsCount1 < paramsCount2) {
			memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, t1, e1, e2);
		}

		for (int i = 0; i < paramsCount1; i++) {
			List<ITypeReference> bounds1 = e1.getFormalTypeParameters().get(i).bounds();

			if (i < paramsCount2) {
				List<ITypeReference> bounds2 = e2.getFormalTypeParameters().get(i).bounds();

				if (!finalExecutable) { // Invariant
					if (!new HashSet<>(bounds1).equals(new HashSet<>(bounds2))) {
						memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, t1, e1, e2);
					}
				} else { // Variance
					// Any new bound that's not a supertype of an existing bound is breaking
					if (bounds2.stream()
						// We can safely ignore this bound
						.filter(b2 -> !b2.equals(TypeReference.OBJECT))
						.anyMatch(b2 -> bounds1.stream().noneMatch(b1 -> v1.isSubtypeOf(b1, b2)))) {
						memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, t1, e1, e2);
					}
				}
			}
		}
	}

	private void typeBC(BreakingChangeKind kind, TypeDecl impactedType, TypeDecl newType) {
		BreakingChange bc = new BreakingChange(kind, impactedType, impactedType, newType);
		breakingChanges.add(bc);
	}

	private void newTypeMemberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl newMember) {
		BreakingChange bc = new BreakingChange(kind, impactedType, impactedType, newMember);
		breakingChanges.add(bc);
	}

	private void memberBC(BreakingChangeKind kind, TypeDecl impactedType,
	                      TypeMemberDecl impactedMember, TypeMemberDecl newMember) {
		// java.lang.Object methods are an absolute pain to handle. Many rules
		// do not apply to them as they're implicitly provided to any class.
		if (impactedMember.getContainingType().equals(TypeReference.OBJECT)) {
			return;
		}
		BreakingChange bc = new BreakingChange(kind, impactedType, impactedMember, newMember);
		breakingChanges.add(bc);
	}

	public List<BreakingChange> getBreakingChanges() {
		return breakingChanges.stream().toList();
	}

	@Override
	public String toString() {
		return breakingChanges.stream()
			.map(BreakingChange::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
