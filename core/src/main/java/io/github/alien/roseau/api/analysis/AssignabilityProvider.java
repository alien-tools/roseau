package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;

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
			return from.getQualifiedName().equals(to.getQualifiedName()) || subtyping().isSubtypeOf(scope, from, to);
		}

		return subtyping().isSubtypeOf(scope, from, to);
	}
}
