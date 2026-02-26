package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Objects;

/**
 * Evaluates Java subtype/containment relations over type references.
 */
public interface SubtypingProvider {
	// Dependencies
	TypeResolver resolver();

	HierarchyProvider hierarchy();

	TypeParameterProvider typeParameter();

	/**
	 * Checks nominal subtyping (fqn-only) between two type references, ignoring generic arguments.
	 *
	 * @param reference the potential subtype
	 * @param other     the potential supertype
	 * @return true if {@code reference} is nominally a subtype of {@code other}
	 */
	default boolean isNominalSubtypeOf(TypeReference<?> reference, TypeReference<?> other) {
		Preconditions.checkNotNull(reference);
		Preconditions.checkNotNull(other);
		return Objects.equals(reference.getQualifiedName(), other.getQualifiedName()) ||
			hierarchy().getAllSuperTypes(reference).stream()
				.anyMatch(sup -> Objects.equals(sup.getQualifiedName(), other.getQualifiedName()));
	}

	/**
	 * Checks subtyping between two references in a given member scope. Resolves type-variable bounds using the supplied
	 * scope and applies wildcard containment rules.
	 *
	 * @param scope     the scope in which to resolve type variable bounds
	 * @param reference the first type reference
	 * @param other     the second type reference
	 * @return true if the first reference is a subtype of the second
	 */
	default boolean isSubtypeOf(TypeParameterScope scope, ITypeReference reference, ITypeReference other) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(reference);
		Preconditions.checkNotNull(other);

		if (reference.equals(other)) {
			return true;
		}

		if (reference instanceof TypeParameterReference fromTp) {
			ITypeReference bound = resolveTypeVariableForSubtyping(scope, fromTp);
			return !bound.equals(reference) && isSubtypeOf(scope, bound, other);
		}

		if (other instanceof TypeParameterReference toTp) {
			ITypeReference bound = resolveTypeVariableForSubtyping(scope, toTp);
			return !bound.equals(other) && isSubtypeOf(scope, reference, bound);
		}

		return switch (reference) {
			case PrimitiveTypeReference fromPrimitive ->
				other instanceof PrimitiveTypeReference toPrimitive && fromPrimitive.equals(toPrimitive);
			case ArrayTypeReference fromArray -> isArraySubtype(scope, fromArray, other);
			case TypeReference<?> fromRef -> isReferenceSubtype(scope, fromRef, other);
			case WildcardTypeReference fromWildcard -> isWildcardSubtype(scope, fromWildcard, other);
			default -> false;
		};
	}

	/**
	 * Checks whether this type reference points to a checked exception type.
	 *
	 * @param reference the reference to check
	 * @return true if the reference points a checked exception
	 */
	default boolean isCheckedException(ITypeReference reference) {
		return reference instanceof TypeReference<?> tr &&
			resolver().resolve(tr).map(this::isCheckedException).orElse(false);
	}

	/**
	 * Checks whether the given type is a checked exception type. Checked exception types are subclasses of
	 * {@link Throwable} but not of {@link RuntimeException}.
	 *
	 * @return whether the current class is a checked exception
	 */
	default boolean isCheckedException(TypeDecl type) {
		return type.isClass() &&
			isSubtypeOf(type, new TypeReference<>(type.getQualifiedName()), TypeReference.THROWABLE) &&
			!isUncheckedException(type);
	}

	/**
	 * Checks whether the given type is an unchecked exception type. Unchecked exception types are subclasses of
	 * either {@link RuntimeException} or {@link Error}.
	 *
	 * @return whether the current class is an unchecked exception
	 */
	default boolean isUncheckedException(TypeDecl type) {
		return type.isClass() &&
			(isSubtypeOf(type, new TypeReference<>(type.getQualifiedName()), TypeReference.RUNTIME_EXCEPTION) ||
				isSubtypeOf(type, new TypeReference<>(type.getQualifiedName()), TypeReference.ERROR));
	}

	private boolean isArraySubtype(TypeParameterScope scope, ArrayTypeReference reference, ITypeReference other) {
		return switch (other) {
			case TypeReference<?> tr -> isArraySuperType(tr);
			case ArrayTypeReference otherArray -> {
				ITypeReference fromComponent = arrayElementType(reference);
				ITypeReference toComponent = arrayElementType(otherArray);

				if (fromComponent instanceof PrimitiveTypeReference || toComponent instanceof PrimitiveTypeReference) {
					yield fromComponent instanceof PrimitiveTypeReference fromPrimitive &&
						toComponent instanceof PrimitiveTypeReference toPrimitive &&
						fromPrimitive.equals(toPrimitive);
				}

				yield isSubtypeOf(scope, fromComponent, toComponent);
			}
			default -> false;
		};
	}

	private static ITypeReference arrayElementType(ArrayTypeReference array) {
		return array.dimension() == 1
			? array.componentType()
			: new ArrayTypeReference(array.componentType(), array.dimension() - 1);
	}

	private static boolean isArraySuperType(TypeReference<?> reference) {
		String fqn = reference.getQualifiedName();
		return Objects.equals(fqn, Object.class.getCanonicalName()) ||
			Objects.equals(fqn, Cloneable.class.getCanonicalName()) ||
			Objects.equals(fqn, java.io.Serializable.class.getCanonicalName());
	}

	private boolean isReferenceSubtype(TypeParameterScope scope, TypeReference<?> reference, ITypeReference other) {
		return switch (other) {
			case TypeReference<?> otherRef -> {
				if (Objects.equals(reference.getQualifiedName(), otherRef.getQualifiedName())) {
					yield areTypeArgumentsContained(scope, reference.typeArguments(), otherRef.typeArguments());
				}

				yield hierarchy().getAllInstantiatedSuperTypes(reference).stream()
					.filter(sup -> Objects.equals(sup.getQualifiedName(), otherRef.getQualifiedName()))
					.anyMatch(sup -> areTypeArgumentsContained(scope, sup.typeArguments(), otherRef.typeArguments()));
			}
			case WildcardTypeReference wildcard -> isTypeArgumentContained(scope, reference, wildcard);
			default -> false;
		};
	}

	private boolean isWildcardSubtype(TypeParameterScope scope, WildcardTypeReference reference, ITypeReference other) {
		if (other instanceof WildcardTypeReference otherWildcard) {
			return isWildcardContained(scope, reference, otherWildcard);
		}
		return false;
	}

	private boolean areTypeArgumentsContained(TypeParameterScope scope,
	                                          List<ITypeReference> fromArgs, List<ITypeReference> toArgs) {
		if (toArgs.isEmpty()) {
			return true;
		}

		if (fromArgs.isEmpty()) {
			return false;
		}

		if (fromArgs.size() != toArgs.size()) {
			return false;
		}

		for (int i = 0; i < fromArgs.size(); i++) {
			if (!isTypeArgumentContained(scope, fromArgs.get(i), toArgs.get(i))) {
				return false;
			}
		}

		return true;
	}

	private boolean isTypeArgumentContained(TypeParameterScope scope, ITypeReference fromArg, ITypeReference toArg) {
		return switch (toArg) {
			case WildcardTypeReference toWildcard -> fromArg instanceof WildcardTypeReference fromWildcard
				? isWildcardContained(scope, fromWildcard, toWildcard)
				: isAssignableToWildcardBound(scope, fromArg, toWildcard);
			default -> fromArg.equals(toArg);
		};
	}

	private boolean isAssignableToWildcardBound(TypeParameterScope scope,
	                                            ITypeReference fromArg, WildcardTypeReference toWildcard) {
		if (toWildcard.isUnbounded()) {
			return true;
		}

		if (toWildcard.upper()) {
			return toWildcard.bounds().stream().allMatch(bound -> isSubtypeOf(scope, fromArg, bound));
		}

		ITypeReference lowerBound = toWildcard.bounds().getFirst();
		return isSubtypeOf(scope, lowerBound, fromArg);
	}

	private boolean isWildcardContained(TypeParameterScope scope,
	                                    WildcardTypeReference reference, WildcardTypeReference other) {
		if (other.isUnbounded()) {
			return true;
		}

		if (reference.isUnbounded()) {
			return false;
		}

		if (reference.upper() != other.upper()) {
			return false;
		}

		if (reference.upper()) {
			return other.bounds().stream()
				.allMatch(otherBound ->
					reference.bounds().stream().anyMatch(refBound -> isSubtypeOf(scope, refBound, otherBound)));
		}

		return isSubtypeOf(scope, other.bounds().getFirst(), reference.bounds().getFirst());
	}

	private ITypeReference resolveTypeVariableForSubtyping(TypeParameterScope scope, TypeParameterReference reference) {
		return typeParameter().resolveTypeParameter(scope, reference)
			.map(tp -> {
				ITypeReference directBound = tp.bounds().getFirst();
				if (directBound.equals(TypeReference.OBJECT)) {
					return reference;
				}
				return directBound instanceof TypeParameterReference tpr
					? resolveTypeVariableForSubtyping(scope, tpr)
					: directBound;
			})
			.orElse(reference);
	}
}
