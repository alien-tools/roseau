package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Computes the list of breaking changes between two {@link API} instances.
 * <br>
 * The compared APIs are visited deeply to match their symbols pairwise based on their unique name and compare their
 * properties when their names match. This implementation visits all {@link TypeDecl} instances in parallel.
 */
public class BreakingChangeAnalyzer implements ApiDiffer<RoseauReport> {
	private final API v1;
	private final API v2;
	private final RoseauReport.Builder builder;

	public BreakingChangeAnalyzer(API v1, API v2) {
		this.v1 = Preconditions.checkNotNull(v1);
		this.v2 = Preconditions.checkNotNull(v2);
		this.builder = RoseauReport.builder(v1, v2);
	}

	@Override
	public RoseauReport get() {
		return this.builder.build();
	}

	@Override
	public void onMatchedType(TypeDecl oldType, TypeDecl newType) {
		if (oldType.isPublic() && newType.isProtected()) {
			builder.typeBC(BreakingChangeKind.TYPE_NOW_PROTECTED, oldType);
		}

		if (!oldType.getClass().equals(newType.getClass())) {
			builder.typeBC(BreakingChangeKind.CLASS_TYPE_CHANGED, oldType,
				new BreakingChangeDetails.ClassTypeChanged(oldType.getClass(), newType.getClass()));
			return; // Avoid all cascading changes
		}

		// If a supertype that was exported has been removed,
		// it may have been used in client code for casts
		List<TypeReference<TypeDecl>> candidates = v1.getAllSuperTypes(oldType).stream()
			.filter(v1::isExported)
			.filter(sup -> !v2.isSubtypeOf(newType, sup))
			.toList();

		// Only report on the closest super type
		candidates.stream()
			.filter(sup -> candidates.stream().noneMatch(other -> !other.equals(sup) && v1.isSubtypeOf(other, sup)))
			.forEach(sup ->
				builder.typeBC(BreakingChangeKind.SUPERTYPE_REMOVED, oldType,
					new BreakingChangeDetails.SuperTypeRemoved(sup)));

		if (oldType instanceof ClassDecl c1 && newType instanceof ClassDecl c2) {
			diffClass(c1, c2);
		}

		if (oldType instanceof AnnotationDecl a1 && newType instanceof AnnotationDecl a2) {
			diffAnnotationInterface(a1, a2);
		}

		diffFormalTypeParameters(oldType, newType);
	}

	private void diffClass(ClassDecl c1, ClassDecl c2) {
		if (!v1.isEffectivelyFinal(c1) && v2.isEffectivelyFinal(c2)) {
			builder.typeBC(BreakingChangeKind.CLASS_NOW_FINAL, c1);
		}

		if (!c1.isEffectivelyAbstract() && c2.isEffectivelyAbstract()) {
			builder.typeBC(BreakingChangeKind.CLASS_NOW_ABSTRACT, c1);
		}

		if (c1.isNested() && c2.isNested()) {
			if (!c1.isStatic() && c2.isStatic()) {
				builder.typeBC(BreakingChangeKind.NESTED_CLASS_NOW_STATIC, c1);
			}

			if (c1.isStatic() && !c2.isStatic()) {
				builder.typeBC(BreakingChangeKind.NESTED_CLASS_NO_LONGER_STATIC, c1);
			}
		}

		if (v1.isUncheckedException(c1) && v2.isCheckedException(c2)) {
			builder.typeBC(BreakingChangeKind.CLASS_NOW_CHECKED_EXCEPTION, c1);
		}
	}

	private void diffAnnotationInterface(AnnotationDecl a1, AnnotationDecl a2) {
		Sets.difference(a1.getTargets(), a2.getTargets())
			.forEach(target ->
				builder.typeBC(BreakingChangeKind.ANNOTATION_TARGET_REMOVED, a1,
					new BreakingChangeDetails.AnnotationTargetRemoved(target))
			);

		if (a1.isRepeatable() && !a2.isRepeatable()) {
			builder.typeBC(BreakingChangeKind.ANNOTATION_NO_LONGER_REPEATABLE, a1);
		}

		a1.getAnnotationMethods().forEach(m1 -> {
			// Annotation methods do not have parameters, so no overloading going on
			//   -> simple name matching should be enough
			Optional<AnnotationMethodDecl> optMatch = a2.getAnnotationMethods().stream()
				.filter(m2 -> m1.getSimpleName().equals(m2.getSimpleName()))
				.findFirst();

			optMatch.ifPresentOrElse(m2 -> {
				if (m1.hasDefault() && !m2.hasDefault()) {
					builder.memberBC(BreakingChangeKind.ANNOTATION_METHOD_NO_LONGER_DEFAULT, a1, m1, m2);
				}

				if (!m1.getType().equals(m2.getType())) {
					builder.memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, a1, m1, m2,
						new BreakingChangeDetails.MethodReturnTypeChanged(m1.getType(), m2.getType()));
				}
			}, () -> builder.memberBC(BreakingChangeKind.METHOD_REMOVED, a1, m1));
		});

		a2.getAnnotationMethods().stream()
			.filter(m2 -> !m2.hasDefault())
			.filter(m2 -> a1.getAnnotationMethods().stream()
				.noneMatch(m1 -> m1.getSimpleName().equals(m2.getSimpleName())))
			.forEach(m2 ->
				builder.typeBC(BreakingChangeKind.ANNOTATION_METHOD_ADDED_WITHOUT_DEFAULT, a1,
					new BreakingChangeDetails.AnnotationMethodAddedWithoutDefault(m2)));
	}

	@Override
	public void onRemovedType(TypeDecl type) {
		builder.typeBC(BreakingChangeKind.TYPE_REMOVED, type);
	}

	@Override
	public void onAddedType(TypeDecl type) {

	}

	@Override
	public void onMatchedField(TypeDecl oldType, TypeDecl newType, FieldDecl oldField, FieldDecl newField) {
		if (!oldField.isFinal() && newField.isFinal()) {
			builder.memberBC(BreakingChangeKind.FIELD_NOW_FINAL, oldType, oldField, newField);
		}

		if (!oldField.isStatic() && newField.isStatic()) {
			builder.memberBC(BreakingChangeKind.FIELD_NOW_STATIC, oldType, oldField, newField);
		}

		if (oldField.isStatic() && !newField.isStatic()) {
			builder.memberBC(BreakingChangeKind.FIELD_NO_LONGER_STATIC, oldType, oldField, newField);
		}

		if (!oldField.getType().equals(newField.getType())) {
			builder.memberBC(BreakingChangeKind.FIELD_TYPE_CHANGED, oldType, oldField, newField,
				new BreakingChangeDetails.FieldTypeChanged(oldField.getType(), newField.getType()));
		}

		if (oldField.isPublic() && newField.isProtected()) {
			builder.memberBC(BreakingChangeKind.FIELD_NOW_PROTECTED, oldType, oldField, newField);
		}
	}

	@Override
	public void onRemovedField(TypeDecl type, FieldDecl field) {
		builder.memberBC(BreakingChangeKind.FIELD_REMOVED, type, field);
	}

	@Override
	public void onAddedField(TypeDecl type, FieldDecl field) {

	}

	@Override
	public void onMatchedMethod(TypeDecl oldType, TypeDecl newType, MethodDecl oldMethod, MethodDecl newMethod) {
		if (!v1.isEffectivelyFinal(oldType, oldMethod) && v2.isEffectivelyFinal(newType, newMethod)) {
			builder.memberBC(BreakingChangeKind.METHOD_NOW_FINAL, oldType, oldMethod, newMethod);
		}

		if (!oldMethod.isStatic() && newMethod.isStatic()) {
			builder.memberBC(BreakingChangeKind.METHOD_NOW_STATIC, oldType, oldMethod, newMethod);
		}

		if (oldMethod.isStatic() && !newMethod.isStatic()) {
			builder.memberBC(BreakingChangeKind.METHOD_NO_LONGER_STATIC, oldType, oldMethod, newMethod);
		}

		if (!oldMethod.isAbstract() && newMethod.isAbstract()) {
			builder.memberBC(BreakingChangeKind.METHOD_NOW_ABSTRACT, oldType, oldMethod, newMethod);
		}

		if (oldMethod.isPublic() && newMethod.isProtected()) {
			builder.memberBC(BreakingChangeKind.METHOD_NOW_PROTECTED, oldType, oldMethod, newMethod);
		}

		if (!oldMethod.getType().equals(newMethod.getType())) {
			builder.memberBC(BreakingChangeKind.METHOD_RETURN_TYPE_CHANGED, oldType, oldMethod, newMethod,
				new BreakingChangeDetails.MethodReturnTypeChanged(oldMethod.getType(), newMethod.getType()));
		}

		diffThrownExceptions(oldType, oldMethod, newMethod);
		diffFormalTypeParameters(oldType, oldMethod, newMethod);
		diffParameters(oldType, oldMethod, newMethod);
	}

	@Override
	public void onRemovedMethod(TypeDecl type, MethodDecl method) {
		builder.memberBC(BreakingChangeKind.METHOD_REMOVED, type, method);
	}

	@Override
	public void onAddedMethod(TypeDecl type, MethodDecl method) {
		if (method.isAbstract()) {
			if (type.isInterface()) {
				builder.typeBC(BreakingChangeKind.METHOD_ADDED_TO_INTERFACE, type,
					new BreakingChangeDetails.MethodAddedToInterface(method));
			}

			if (type.isClass()) {
				builder.typeBC(BreakingChangeKind.METHOD_ABSTRACT_ADDED_TO_CLASS, type,
					new BreakingChangeDetails.MethodAbstractAddedToClass(method));
			}
		}
	}

	@Override
	public void onMatchedConstructor(ClassDecl oldCls, ClassDecl newCls, ConstructorDecl oldCons, ConstructorDecl newCons) {
		if (oldCons.isPublic() && newCons.isProtected()) {
			builder.memberBC(BreakingChangeKind.CONSTRUCTOR_NOW_PROTECTED, oldCls, oldCons, newCons);
		}

		diffThrownExceptions(oldCls, oldCons, newCons);
		diffFormalTypeParameters(oldCls, oldCons, newCons);
		diffParameters(oldCls, oldCons, newCons);
	}

	@Override
	public void onRemovedConstructor(ClassDecl cls, ConstructorDecl cons) {
		builder.memberBC(BreakingChangeKind.CONSTRUCTOR_REMOVED, cls, cons);
	}

	@Override
	public void onAddedConstructor(ClassDecl cls, ConstructorDecl cons) {

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
		Set<ITypeReference> thrown1 = v1.getThrownCheckedExceptions(e1);
		Set<ITypeReference> thrown2 = v2.getThrownCheckedExceptions(e2);

		thrown1.stream()
			.filter(exc1 -> thrown2.stream().noneMatch(exc2 ->
				// FIXME: correct, but meh
				v2.isSubtypeOf(exc1, exc2) || v1.isEffectivelyFinal(t1, e1)
			))
			.forEach(exc1 ->
				builder.memberBC(BreakingChangeKind.METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION, t1, e1, e2,
					new BreakingChangeDetails.MethodNoLongerThrowsCheckedException(exc1)));

		thrown2.stream()
			.filter(exc2 -> thrown1.stream().noneMatch(exc1 -> v2.isSubtypeOf(exc2, exc1)))
			.forEach(exc2 ->
				builder.memberBC(BreakingChangeKind.METHOD_NOW_THROWS_CHECKED_EXCEPTION, t1, e1, e2,
					new BreakingChangeDetails.MethodNowThrowsCheckedException(exc2)));
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
	 * sub/super-classes match), and the latter cannot (thus parameters can follow variance rules).
	 */
	private void diffParameterGenerics(TypeDecl t1, ExecutableDecl e1, ExecutableDecl e2,
	                                   TypeReference<?> pt1, TypeReference<?> pt2) {
		BreakingChangeDetails details =
			new BreakingChangeDetails.MethodParameterGenericsChanged(pt1, pt2);

		if (pt1.typeArguments().size() != pt2.typeArguments().size()) {
			builder.memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, t1, e1, e2, details);
			return;
		}

		boolean isFinalExecutable = v1.isEffectivelyFinal(t1, e1);
		// Can be overridden = invariant
		if (!isFinalExecutable && !pt1.equals(pt2)) {
			builder.memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, t1, e1, e2, details);
		}

		// Can't = variance
		if (isFinalExecutable && !v1.isSubtypeOf(pt1, pt2)) {
			builder.memberBC(BreakingChangeKind.METHOD_PARAMETER_GENERICS_CHANGED, t1, e1, e2, details);
		}
	}

	private void diffFormalTypeParameters(TypeDecl t1, TypeDecl t2) {
		int paramsCount1 = t1.getFormalTypeParameters().size();
		int paramsCount2 = t2.getFormalTypeParameters().size();

		// Removing formal type parameters always breaks
		if (paramsCount1 > paramsCount2) {
			t1.getFormalTypeParameters().subList(paramsCount2, paramsCount1)
				.forEach(ftp ->
					builder.typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_REMOVED, t1,
						new BreakingChangeDetails.TypeFormalTypeParametersRemoved(ftp)));
			return;
		}

		// Adding formal type parameters breaks unless it's the first
		if (paramsCount2 > paramsCount1 && paramsCount1 > 0) {
			t2.getFormalTypeParameters().subList(paramsCount1, paramsCount2)
				.forEach(ftp ->
					builder.typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_ADDED, t1,
						new BreakingChangeDetails.TypeFormalTypeParametersAdded(ftp)));
			return;
		}

		for (int i = 0; i < paramsCount1; i++) {
			FormalTypeParameter p1 = t1.getFormalTypeParameters().get(i);
			FormalTypeParameter p2 = t2.getFormalTypeParameters().get(i);

			// Each bound in the new version should be a supertype (inclusive) of an existing one
			// so that the type constraints imposed by p1 are stricter than those imposed by p2
			if (p2.bounds().stream()
				.anyMatch(b2 -> !b2.equals(TypeReference.OBJECT) &&
					p1.bounds().stream().noneMatch(b1 -> v2.isSubtypeOf(b1, b2)))) {
				builder.typeBC(BreakingChangeKind.TYPE_FORMAL_TYPE_PARAMETERS_CHANGED, t1,
					new BreakingChangeDetails.TypeFormalTypeParametersChanged(p1, p2));
			}
		}
	}

	private void diffFormalTypeParameters(TypeDecl t1, ExecutableDecl e1, ExecutableDecl e2) {
		int paramsCount1 = e1.getFormalTypeParameters().size();
		int paramsCount2 = e2.getFormalTypeParameters().size();
		boolean isOverridable = !v1.isEffectivelyFinal(t1, e1);

		// Removing a type parameter is breaking if:
		//  - it's a method (due to @Override)
		//  - it's a constructor and there was more than one
		if (paramsCount1 > paramsCount2 && (isOverridable || paramsCount1 > 1)) {
			e1.getFormalTypeParameters().subList(paramsCount2, paramsCount1)
				.forEach(ftp ->
					builder.memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_REMOVED, t1, e1, e2,
						new BreakingChangeDetails.MethodFormalTypeParametersRemoved(ftp)));
			return;
		}

		// Adding a type parameter is only breaking if there was already some
		if (paramsCount1 > 0 && paramsCount1 < paramsCount2) {
			e2.getFormalTypeParameters().subList(paramsCount1, paramsCount2)
				.forEach(ftp ->
					builder.memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_ADDED, t1, e1, e2,
						new BreakingChangeDetails.MethodFormalTypeParametersAdded(ftp)));
			return;
		}

		for (int i = 0; i < paramsCount1; i++) {
			FormalTypeParameter ftp1 = e1.getFormalTypeParameters().get(i);
			List<ITypeReference> bounds1 = ftp1.bounds();

			if (i < paramsCount2) {
				FormalTypeParameter ftp2 = e2.getFormalTypeParameters().get(i);
				List<ITypeReference> bounds2 = ftp2.bounds();

				if (isOverridable) { // Invariant
					if (!new HashSet<>(bounds1).equals(new HashSet<>(bounds2))) {
						builder.memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, t1, e1, e2,
							new BreakingChangeDetails.MethodFormalTypeParametersChanged(ftp1, ftp2));
					}
				} else { // Variance
					// Any new bound that's not a supertype of an existing bound is breaking
					if (bounds2.stream()
						// We can safely ignore this bound
						.filter(b2 -> !b2.equals(TypeReference.OBJECT))
						.anyMatch(b2 -> bounds1.stream().noneMatch(b1 -> v1.isSubtypeOf(b1, b2)))) {
						builder.memberBC(BreakingChangeKind.METHOD_FORMAL_TYPE_PARAMETERS_CHANGED, t1, e1, e2,
							new BreakingChangeDetails.MethodFormalTypeParametersChanged(ftp1, ftp2));
					}
				}
			}
		}
	}
}
