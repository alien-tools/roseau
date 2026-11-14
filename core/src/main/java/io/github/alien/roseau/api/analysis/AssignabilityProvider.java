package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

import java.util.List;
import java.util.stream.Stream;

/**
 * Evaluates Java assignment compatibility between types.
 */
public interface AssignabilityProvider {
	// Dependencies
	SubtypingProvider subtyping();

	TypeParameterProvider typeParameter();

	/**
	 * Checks whether a value of type {@code from} can be assigned to a variable of type {@code to} in the given scope.
	 *
	 * @param scope the scope in which to resolve type variable bounds
	 * @param from  the type of the value to check
	 * @param to    the type of the variable to check
	 * @return true if the value can be assigned to the variable
	 */
	default boolean isAssignable(TypeParameterScope scope, ITypeReference from, ITypeReference to) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(from);
		Preconditions.checkNotNull(to);

		if (from.equals(to)) {
			return true;
		}

		if (from instanceof TypeParameterReference fromTp) {
			// Bounds drive JLS assignment for type variables.
			// We only expand one declaration step to preserve relations like U extends T.
			ITypeReference bound = typeParameter().resolveDirectTypeParameterBound(scope, fromTp);
			return !bound.equals(from) && isAssignable(scope, bound, to);
		}

		if (to instanceof TypeParameterReference) {
			// Assigning to an unresolved type variable would require full capture/constraint solving.
			// For compatibility analysis we stay conservative: not guaranteed assignable.
			return false;
		}

		if (from instanceof PrimitiveTypeReference fromPrimitive) {
			return switch (to) {
				case PrimitiveTypeReference toPrimitive -> isPrimitiveWidening(fromPrimitive, toPrimitive);
				case TypeReference<?> toRef -> {
					TypeReference<?> boxed = box(fromPrimitive);
					yield boxed != null && isReferenceAssignable(scope, boxed, toRef);
				}
				default -> false;
			};
		}

		if (to instanceof PrimitiveTypeReference toPrimitive) {
			return switch (from) {
				case TypeReference<?> fromRef -> {
					PrimitiveTypeReference unboxed = unbox(fromRef);
					yield unboxed != null && (unboxed.equals(toPrimitive) || isPrimitiveWidening(unboxed, toPrimitive));
				}
				default -> false;
			};
		}

		return switch (from) {
			case TypeReference<?> fromRef when to instanceof TypeReference<?> toRef ->
				isReferenceAssignable(scope, fromRef, toRef);
			case PrimitiveTypeReference _ -> false;
			default -> subtyping().isSubtypeOf(scope, from, to);
		};
	}

	/**
	 * Checks whether replacing an expression of type {@code oldType} by one of type {@code newType} preserves
	 * compatibility for previously valid clients, which assign its value and use its members. Although they are
	 * assignable to each other, a raw type and a parameterized one do not have the same members: a parameterized type
	 * loses its type arguments when it becomes raw ({@code String s = list.get(0)}), and a raw type no longer accepts
	 * every value when it becomes parameterized ({@code list.add(1)}).
	 */
	default boolean isSourceCompatibleExpression(TypeParameterScope scope, ITypeReference oldType, ITypeReference newType) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(oldType);
		Preconditions.checkNotNull(newType);

		if (PrimitiveTypeReference.VOID.equals(oldType)) {
			// A void expression yields no value, so its result was never assigned and every change is safe
			return true;
		}

		if (!isAssignable(scope, newType, oldType)) {
			return false;
		}

		if (oldType instanceof PrimitiveTypeReference oldPrimitive) {
			TypeReference<?> boxed = box(oldPrimitive);
			return boxed == null || isAssignable(scope, newType, boxed);
		}

		return isParameterizedAs(newType, oldType) == isParameterized(oldType);
	}

	/**
	 * Checks whether replacing a variable of type {@code oldType} by one of type {@code newType} preserves the
	 * assignments of previously valid clients: every value that was assignable to it must still be. Any
	 * parameterization can be assigned to a raw type, so, once parameterized, it only accepts them all with unbounded
	 * wildcards.
	 */
	default boolean isSourceCompatibleVariable(TypeParameterScope scope, ITypeReference oldType, ITypeReference newType) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(oldType);
		Preconditions.checkNotNull(newType);
		return isAssignable(scope, oldType, newType) &&
			(isParameterizedAs(oldType, newType) || !isParameterized(newType) || isUnboundedParameterization(newType));
	}

	private static boolean isParameterized(ITypeReference type) {
		return switch (type) {
			case TypeReference<?> reference -> !reference.typeArguments().isEmpty();
			case ArrayTypeReference(var componentType, _) -> isParameterized(componentType);
			default -> false;
		};
	}

	private static boolean isUnboundedParameterization(ITypeReference type) {
		return switch (type) {
			case TypeReference<?> reference -> reference.typeArguments().stream()
				.allMatch(arg -> arg instanceof WildcardTypeReference wildcard && wildcard.isUnbounded());
			case ArrayTypeReference(var componentType, _) -> isUnboundedParameterization(componentType);
			default -> false;
		};
	}

	/**
	 * Checks whether {@code type} is parameterized when it is seen as {@code target}, i.e., whether {@code type} or
	 * its supertype that refers to the same type as {@code target} has type arguments. Unrelated types are seen as
	 * {@code target} itself.
	 */
	private boolean isParameterizedAs(ITypeReference type, ITypeReference target) {
		return switch (type) {
			case TypeReference<?> reference when target instanceof TypeReference<?> targetReference ->
				Stream.concat(Stream.of(reference), subtyping().hierarchy().getAllInstantiatedSuperTypes(reference).stream())
					.filter(superType -> superType.getQualifiedName().equals(targetReference.getQualifiedName()))
					.findFirst()
					.map(AssignabilityProvider::isParameterized)
					.orElseGet(() -> isParameterized(target));
			case ArrayTypeReference(var componentType, var dimension)
				when target instanceof ArrayTypeReference(var targetComponentType, var targetDimension) &&
				dimension == targetDimension -> isParameterizedAs(componentType, targetComponentType);
			default -> isParameterized(target);
		};
	}

	private static boolean isPrimitiveWidening(PrimitiveTypeReference from, PrimitiveTypeReference to) {
		// JLS 5.1.2
		return switch (from.name()) {
			case "byte" -> List.of("short", "int", "long", "float", "double").contains(to.name());
			case "short" -> List.of("int", "long", "float", "double").contains(to.name());
			case "char" -> List.of("int", "long", "float", "double").contains(to.name());
			case "int" -> List.of("long", "float", "double").contains(to.name());
			case "long" -> List.of("float", "double").contains(to.name());
			case "float" -> "double".equals(to.name());
			default -> false;
		};
	}

	private static TypeReference<?> box(PrimitiveTypeReference primitive) {
		// JLS 5.1.7
		return switch (primitive.name()) {
			case "boolean" -> new TypeReference<>(Boolean.class.getCanonicalName());
			case "byte" -> new TypeReference<>(Byte.class.getCanonicalName());
			case "short" -> new TypeReference<>(Short.class.getCanonicalName());
			case "char" -> new TypeReference<>(Character.class.getCanonicalName());
			case "int" -> new TypeReference<>(Integer.class.getCanonicalName());
			case "long" -> new TypeReference<>(Long.class.getCanonicalName());
			case "float" -> new TypeReference<>(Float.class.getCanonicalName());
			case "double" -> new TypeReference<>(Double.class.getCanonicalName());
			default -> null;
		};
	}

	private static PrimitiveTypeReference unbox(TypeReference<?> reference) {
		// JLS 5.1.8
		return switch (reference.getQualifiedName()) {
			case "java.lang.Boolean" -> PrimitiveTypeReference.BOOLEAN;
			case "java.lang.Byte" -> PrimitiveTypeReference.BYTE;
			case "java.lang.Short" -> PrimitiveTypeReference.SHORT;
			case "java.lang.Character" -> PrimitiveTypeReference.CHAR;
			case "java.lang.Integer" -> PrimitiveTypeReference.INT;
			case "java.lang.Long" -> PrimitiveTypeReference.LONG;
			case "java.lang.Float" -> PrimitiveTypeReference.FLOAT;
			case "java.lang.Double" -> PrimitiveTypeReference.DOUBLE;
			default -> null;
		};
	}

	private boolean isReferenceAssignable(TypeParameterScope scope, TypeReference<?> from, TypeReference<?> to) {
		if (from.typeArguments().isEmpty() || to.typeArguments().isEmpty()) {
			// Raw/parameterized assignment is allowed via unchecked conversion in either direction.
			return from.getQualifiedName().equals(to.getQualifiedName()) || subtyping().isNominalSubtypeOf(from, to);
		}

		return subtyping().isSubtypeOf(scope, from, to);
	}
}
