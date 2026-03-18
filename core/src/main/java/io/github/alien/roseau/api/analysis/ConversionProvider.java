package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;

/**
 * Evaluates whether changing a consumed type preserves compatibility in assignment and invocation contexts.
 * <p>
 * Roseau uses these predicates for API changes such as field writes and method arguments: given an old consumed type
 * and a new consumed type, do all source types accepted by the old declaration remain accepted by the new one?
 * Primitive cases follow the conversions of JLS 5.2 and JLS 5.3; reference cases reduce to subtyping because all
 * reference values accepted by the old declaration must still be accepted by the new one.
 */
public interface ConversionProvider {
	// Dependencies
	default ConversionProvider conversion() {
		return this;
	}

	SubtypingProvider subtyping();

	TypeParameterProvider typeParameter();

	/**
	 * Checks assignment-context compatibility for a consumed type change.
	 * <p>
	 * This is based on JLS 5.2: if a declaration consumed values of type {@code oldType} before the API change and now
	 * consumes values of type {@code newType}, this method answers whether every source type previously accepted remains
	 * accepted after the change.
	 *
	 * @param scope   the scope in which to resolve type-variable bounds
	 * @param oldType the old consumed type
	 * @param newType the new consumed type
	 * @return true if the new declaration preserves assignment compatibility for previously valid source types
	 */
	default boolean isAssignmentCompatible(TypeParameterScope scope, ITypeReference oldType, ITypeReference newType) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(oldType);
		Preconditions.checkNotNull(newType);

		if (oldType.equals(newType)) {
			return true;
		}

		if (oldType instanceof PrimitiveTypeReference oldPrimitive) {
			return acceptedPrimitiveSources(oldPrimitive).stream()
				.allMatch(source -> acceptsInAssignmentContext(scope, source, newType));
		}

		return !(newType instanceof PrimitiveTypeReference) && subtyping().isSubtypeOf(scope, oldType, newType);
	}

	/**
	 * Checks method-invocation compatibility for a consumed type change.
	 * <p>
	 * This is based on JLS 5.3: if a formal parameter changed from {@code oldType} to {@code newType}, this method
	 * answers whether every source type previously accepted at call sites remains accepted after the change.
	 *
	 * @param scope   the scope in which to resolve type-variable bounds
	 * @param oldType the old consumed type
	 * @param newType the new consumed type
	 * @return true if the new declaration preserves invocation compatibility for previously valid source types
	 */
	default boolean isInvocationCompatible(TypeParameterScope scope, ITypeReference oldType, ITypeReference newType) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(oldType);
		Preconditions.checkNotNull(newType);

		if (oldType.equals(newType)) {
			return true;
		}

		if (oldType instanceof PrimitiveTypeReference oldPrimitive) {
			return acceptedPrimitiveSources(oldPrimitive).stream()
				.allMatch(source -> acceptsInInvocationContext(scope, source, newType));
		}

		return !(newType instanceof PrimitiveTypeReference) && subtyping().isSubtypeOf(scope, oldType, newType);
	}

	/**
	 * Checks whether a value of type {@code source} is accepted by a target of type {@code target} in an assignment
	 * context, following JLS 5.2.
	 *
	 * @param scope  the scope in which to resolve type-variable bounds
	 * @param source the source type
	 * @param target the target type
	 * @return true if assignment conversion accepts the value
	 */
	default boolean acceptsInAssignmentContext(TypeParameterScope scope, ITypeReference source, ITypeReference target) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(target);

		if (source.equals(target)) {
			return true;
		}

		if (source instanceof TypeParameterReference fromTp) {
			ITypeReference directBound = typeParameter().resolveDirectTypeParameterBound(scope, fromTp);
			return !directBound.equals(source) && acceptsInAssignmentContext(scope, directBound, target);
		}

		if (target instanceof TypeParameterReference) {
			return false;
		}

		if (source instanceof PrimitiveTypeReference fromPrimitive) {
			return switch (target) {
				case PrimitiveTypeReference toPrimitive -> isPrimitiveWidening(fromPrimitive, toPrimitive);
				case TypeReference<?> toReference -> {
					TypeReference<?> boxed = box(fromPrimitive);
					yield boxed != null && acceptsReferenceAssignment(scope, boxed, toReference);
				}
				default -> false;
			};
		}

		if (target instanceof PrimitiveTypeReference toPrimitive) {
			return switch (source) {
				case TypeReference<?> fromReference -> {
					PrimitiveTypeReference unboxed = unbox(fromReference);
					yield unboxed != null && (unboxed.equals(toPrimitive) || isPrimitiveWidening(unboxed, toPrimitive));
				}
				default -> false;
			};
		}

		return switch (source) {
			case TypeReference<?> fromReference when target instanceof TypeReference<?> toReference ->
				acceptsReferenceAssignment(scope, fromReference, toReference);
			case PrimitiveTypeReference _ -> false;
			default -> subtyping().isSubtypeOf(scope, source, target);
		};
	}

	/**
	 * Checks whether a value of type {@code source} is accepted by a target of type {@code target} in a method
	 * invocation context, following JLS 5.3.
	 *
	 * @param scope  the scope in which to resolve type-variable bounds
	 * @param source the source type
	 * @param target the target type
	 * @return true if method-invocation conversion accepts the value
	 */
	default boolean acceptsInInvocationContext(TypeParameterScope scope, ITypeReference source, ITypeReference target) {
		// Roseau reasons over source types, not concrete literals. At that level, method-invocation conversion differs
		// from assignment conversion only for value-dependent constant narrowing, which is not modelled here.
		return acceptsInAssignmentContext(scope, source, target);
	}

	/**
	 * Returns the boxed type for the given primitive, as defined by JLS 5.1.7.
	 *
	 * @param primitive the primitive type to box
	 * @return the boxed reference type, or {@code null} for {@code void}
	 */
	default TypeReference<?> box(PrimitiveTypeReference primitive) {
		Preconditions.checkNotNull(primitive);

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

	/**
	 * Returns the primitive type obtained by unboxing the given wrapper type, as defined by JLS 5.1.8.
	 *
	 * @param reference the wrapper reference type to unbox
	 * @return the unboxed primitive type, or {@code null} if the type is not a wrapper
	 */
	default PrimitiveTypeReference unbox(TypeReference<?> reference) {
		Preconditions.checkNotNull(reference);

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

	/**
	 * Checks whether the reference denotes a raw type under JLS 4.8.
	 *
	 * @param reference the type reference to inspect
	 * @return true if the reference is raw
	 */
	default boolean isRawType(ITypeReference reference) {
		Preconditions.checkNotNull(reference);

		return switch (reference) {
			case ArrayTypeReference array -> isRawType(array.componentType());
			case TypeReference<?> typeReference -> typeReference.typeArguments().isEmpty() &&
				subtyping().resolver().resolve(typeReference)
					.map(TypeDecl::getFormalTypeParameters)
					.map(params -> !params.isEmpty())
					.orElse(false);
			default -> false;
		};
	}

	/**
	 * Checks whether converting a return type from {@code source} to {@code target} may rely on unchecked conversion.
	 *
	 * @param scope  the scope in which to resolve type-variable bounds
	 * @param source the source return type
	 * @param target the target return type
	 * @return true if unchecked conversion can make the return type substitutable
	 */
	default boolean canConvertByUncheckedConversion(TypeParameterScope scope, ITypeReference source, ITypeReference target) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(source);
		Preconditions.checkNotNull(target);

		if (source instanceof ArrayTypeReference fromArray && target instanceof ArrayTypeReference toArray) {
			return canConvertByUncheckedConversion(scope, fromArray.componentType(), toArray.componentType());
		}

		if (!(source instanceof TypeReference<?> fromReference) || !(target instanceof TypeReference<?> toReference)) {
			return false;
		}

		if (fromReference.typeArguments().isEmpty() || toReference.typeArguments().isEmpty()) {
			return fromReference.getQualifiedName().equals(toReference.getQualifiedName()) ||
				subtyping().isNominalSubtypeOf(fromReference, toReference);
		}

		return false;
	}

	private List<PrimitiveTypeReference> acceptedPrimitiveSources(PrimitiveTypeReference target) {
		return List.of(
			PrimitiveTypeReference.BOOLEAN,
			PrimitiveTypeReference.BYTE,
			PrimitiveTypeReference.SHORT,
			PrimitiveTypeReference.CHAR,
			PrimitiveTypeReference.INT,
			PrimitiveTypeReference.LONG,
			PrimitiveTypeReference.FLOAT,
			PrimitiveTypeReference.DOUBLE
		).stream()
			.filter(source -> source.equals(target) || isPrimitiveWidening(source, target))
			.toList();
	}

	private boolean acceptsReferenceAssignment(TypeParameterScope scope, TypeReference<?> source, TypeReference<?> target) {
		if (source.typeArguments().isEmpty() || target.typeArguments().isEmpty()) {
			return source.getQualifiedName().equals(target.getQualifiedName()) ||
				subtyping().isNominalSubtypeOf(source, target);
		}

		return subtyping().isSubtypeOf(scope, source, target);
	}

	private static boolean isPrimitiveWidening(PrimitiveTypeReference from, PrimitiveTypeReference to) {
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
}
