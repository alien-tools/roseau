package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface SubtypingProvider {
	// Dependencies
	TypeResolver resolver();

	HierarchyProvider hierarchy();

	/**
	 * Checks whether the provided type is a subtype of the supplied type reference. Types are subtypes of themselves,
	 * {@link TypeReference#OBJECT}, and all types they implement or extend.
	 *
	 * @param type      the type to check
	 * @param reference the type reference to check for subtyping
	 * @return true if the type is a subtype of the referenced type
	 */
	default boolean isSubtypeOf(TypeDecl type, ITypeReference reference) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(reference);
		return reference.equals(TypeReference.OBJECT) ||
			Objects.equals(type.getQualifiedName(), reference.getQualifiedName()) ||
			hierarchy().getAllSuperTypes(type).stream()
				.anyMatch(sup -> Objects.equals(sup.getQualifiedName(), reference.getQualifiedName()));
	}

	/**
	 * Checks whether the referenced type is a subtype of another referenced type. Types are subtypes of themselves,
	 * {@link TypeReference#OBJECT}, and all types they implement or extend.
	 *
	 * @param reference the referenced type
	 * @param other     the referenced type to check for subtyping
	 * @return true if the referenced types are subtypes of each other
	 */
	default boolean isSubtypeOf(ITypeReference reference, ITypeReference other) {
		Preconditions.checkNotNull(reference);
		Preconditions.checkNotNull(other);

		// FIXME: are primitive/array/wildcard actually subtypes of OBJECT?
		if (reference.equals(other) || other.equals(TypeReference.OBJECT)) {
			return true;
		}

		return switch (reference) {
			case ArrayTypeReference atr ->
				other instanceof ArrayTypeReference otherAtr && checkArrayTypeSubtyping(atr, otherAtr);
			case PrimitiveTypeReference ptr ->
				other instanceof PrimitiveTypeReference otherPtr && checkPrimitiveTypeSubtyping(ptr, otherPtr);
			case TypeParameterReference tpr ->
				other instanceof TypeParameterReference otherTpr && checkTypeParameterSubtyping(tpr, otherTpr);
			case WildcardTypeReference wtr ->
				other instanceof WildcardTypeReference otherWtr && checkWildcardTypeSubtyping(wtr, otherWtr);
			case TypeReference<?> tr -> checkTypeSubtyping(tr, other);
		};
	}

	private boolean checkArrayTypeSubtyping(ArrayTypeReference reference, ArrayTypeReference other) {
		return reference.dimension() == other.dimension() &&
			isSubtypeOf(reference.componentType(), other.componentType());
	}

	private boolean checkPrimitiveTypeSubtyping(PrimitiveTypeReference reference, PrimitiveTypeReference other) {
		// Narrowing is fine, widening isn't
		return switch (reference.name()) {
			case "byte" -> List.of("short", "int", "long", "float", "double").contains(other.name());
			case "short", "char" -> List.of("int", "long", "float", "double").contains(other.name());
			case "int" -> List.of("long", "float", "double").contains(other.name());
			case "long" -> List.of("float", "double").contains(other.name());
			case "float" -> "double".equals(other.name());
			default -> false;
		};
	}

	private boolean checkTypeParameterSubtyping(TypeParameterReference reference, TypeParameterReference other) {
		return Objects.equals(reference.name(), other.name());
	}

	private boolean checkWildcardTypeSubtyping(WildcardTypeReference reference, WildcardTypeReference other) {
		// Always subtype of unbounded wildcard
		if (other.isUnbounded()) {
			return true;
		}

		if (reference.upper()) {
			// Upper bounds can be made weaker
			return other.upper() && hasStricterBoundsThan(reference, other);
		} else {
			// Changing the (one) lower bound to a subtype is fine
			return !other.upper() && isSubtypeOf(other.bounds().getFirst(), reference.bounds().getFirst());
		}
	}

	private boolean checkTypeSubtyping(TypeReference<?> reference, ITypeReference other) {
		return switch (other) {
			// FIXME: what if upper() or !upper()?
			case WildcardTypeReference wtr -> wtr.bounds().stream().allMatch(b -> isSubtypeOf(reference, b));
			case TypeReference<?> tr -> Stream.concat(Stream.of(reference), hierarchy().getAllSuperTypes(reference).stream())
				.anyMatch(sup -> Objects.equals(sup.getQualifiedName(), tr.getQualifiedName()) &&
					hasCompatibleTypeParameters(reference, tr));
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
		return type.isClass() && isSubtypeOf(type, TypeReference.THROWABLE) && !isUncheckedException(type);
	}

	/**
	 * Checks whether the given type is an unchecked exception type. Unchecked exception types are subclasses of
	 * either {@link RuntimeException} or {@link Error}.
	 *
	 * @return whether the current class is an unchecked exception
	 */
	default boolean isUncheckedException(TypeDecl type) {
		return type.isClass() &&
			(isSubtypeOf(type, TypeReference.RUNTIME_EXCEPTION) || isSubtypeOf(type, TypeReference.ERROR));
	}

	/**
	 * Checks whether these bounds are stricter than the bounds of another wildcard
	 */
	private boolean hasStricterBoundsThan(WildcardTypeReference reference, WildcardTypeReference other) {
		return other.bounds().stream()
			.allMatch(otherBound ->
				reference.bounds().stream()
					.anyMatch(refBound -> isSubtypeOf(refBound, otherBound)));
	}

	private boolean hasCompatibleTypeParameters(TypeReference<?> reference, TypeReference<?> other) {
		if (reference.typeArguments().size() != other.typeArguments().size()) {
			return false;
		}

		return IntStream.range(0, reference.typeArguments().size())
			.allMatch(i -> isSubtypeOf(reference.typeArguments().get(i), other.typeArguments().get(i)));
	}
}
