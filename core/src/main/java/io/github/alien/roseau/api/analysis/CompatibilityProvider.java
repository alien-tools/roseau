package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;

/**
 * Evaluates high-level source-compatibility predicates used by Roseau's breaking-change rules.
 */
public interface CompatibilityProvider extends ConversionProvider {
	// Dependencies
	default CompatibilityProvider compatibility() {
		return this;
	}

	SubtypingProvider subtyping();

	/**
	 * Checks whether replacing an expression of type {@code oldType} by one of type {@code newType} preserves the
	 * validity of previously well-typed client expressions.
	 * <p>
	 * This is Roseau's abstraction for produced expressions such as field reads and method results. It is grounded in
	 * JLS typing and conversion rules, but it is intentionally stricter than plain assignment conversion because client
	 * code can keep using the resulting expression directly. In particular, raw-expression compatibility requires the
	 * replacement to stay raw, and primitive expressions must still work both in their primitive contexts and in the
	 * boxing contexts available before the change.
	 *
	 * @param scope   the scope in which to resolve type-variable bounds
	 * @param oldType the old produced type
	 * @param newType the new produced type
	 * @return true if the new expression preserves source compatibility for previously valid client uses
	 */
	default boolean isExpressionCompatible(TypeParameterScope scope, ITypeReference oldType, ITypeReference newType) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(oldType);
		Preconditions.checkNotNull(newType);

		if (oldType.equals(newType)) {
			return true;
		}

		if (oldType.equals(PrimitiveTypeReference.VOID) || newType.equals(PrimitiveTypeReference.VOID)) {
			return oldType.equals(newType);
		}

		if (oldType instanceof PrimitiveTypeReference oldPrimitive) {
			var boxedOld = box(oldPrimitive);
			return boxedOld != null &&
				acceptsInAssignmentContext(scope, newType, oldPrimitive) &&
				acceptsInAssignmentContext(scope, newType, boxedOld);
		}

		if (isRawType(oldType)) {
			return isRawType(newType) &&
				subtyping().isSubtypeOf(scope, newType, oldType);
		}

		return subtyping().isSubtypeOf(scope, newType, oldType);
	}

	/**
	 * Checks return-type-substitutability for method overriding.
	 * <p>
	 * This follows JLS 8.4.5: an existing overriding return type {@code oldType} remains valid after a superclass or
	 * interface method changes its return type to {@code newType} if the old return type is still return-type-
	 * substitutable for the new one. Primitive and {@code void} return types must therefore remain identical, while
	 * reference return types may rely on covariance or unchecked conversion.
	 *
	 * @param scope   the scope in which to resolve type-variable bounds
	 * @param oldType the return type used by existing overriding methods
	 * @param newType the new return type declared in the overridden method
	 * @return true if existing overriding methods remain source-compatible
	 */
	default boolean isReturnTypeSubstitutable(TypeParameterScope scope, ITypeReference oldType, ITypeReference newType) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(oldType);
		Preconditions.checkNotNull(newType);

		if (oldType.equals(newType)) {
			return true;
		}

		if (oldType instanceof PrimitiveTypeReference || newType instanceof PrimitiveTypeReference) {
			return false;
		}

		return subtyping().isSubtypeOf(scope, oldType, newType) ||
			canConvertByUncheckedConversion(scope, oldType, newType);
	}
}
