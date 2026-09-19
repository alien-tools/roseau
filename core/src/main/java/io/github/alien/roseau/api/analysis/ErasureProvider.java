package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Objects;
import java.util.function.UnaryOperator;

public interface ErasureProvider {
	// Dependencies
	TypeResolver resolver();

	TypeParameterProvider typeParameter();

	/**
	 * Returns the unqualified erasure of the signature of an executable as specified in JLS §4.6. Parameter types are
	 * replaced with their erasure.
	 *
	 * @param executable the executable
	 * @return the executable's erasure
	 */
	default String getErasure(ExecutableDecl executable) {
		Preconditions.checkNotNull(executable);
		return computeErasure(executable, type -> getErasedType(executable, type));
	}

	/**
	 * Returns the unqualified erasure of an executable as seen from {@code owner}, the type it is a member of.
	 * Members inherited from a generic supertype are instantiated with the type arguments {@code owner} supplies,
	 * so their type variables may be declared by {@code owner} instead of by their declaring type.
	 *
	 * @param owner      the type the executable is a member of
	 * @param executable the executable
	 * @return the executable's erasure
	 * @see #getErasure(ExecutableDecl)
	 */
	default String getErasure(TypeDecl owner, ExecutableDecl executable) {
		Preconditions.checkNotNull(owner);
		Preconditions.checkNotNull(executable);
		return computeErasure(executable, type -> getErasedType(owner, executable, type));
	}

	/**
	 * Checks whether two executables have the same erasure. Either executable may be a member inherited by the other's
	 * type, in which case its type variables are declared by that type rather than by its own declaring type.
	 *
	 * @param e1 the first executable
	 * @param e2 the second executable
	 * @return true if both executables have the same erasure
	 */
	default boolean haveSameErasure(ExecutableDecl e1, ExecutableDecl e2) {
		Preconditions.checkNotNull(e1);
		Preconditions.checkNotNull(e2);
		return Objects.equals(getErasureIn(e1, e2), getErasureIn(e2, e1));
	}

	/**
	 * Returns the erased form of the given type reference within the context of the specified scope.
	 *
	 * @param scope     the scope defining the resolution context for type parameters
	 * @param reference the type reference to be erased
	 * @return the erased form of the supplied type reference
	 */
	default ITypeReference getErasedType(TypeParameterScope scope, ITypeReference reference) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(reference);
		return switch (reference) {
			case TypeParameterReference tpr -> typeParameter().resolveTypeParameterBound(scope, tpr);
			case ArrayTypeReference(var t, var dimension) -> new ArrayTypeReference(getErasedType(scope, t), dimension);
			case TypeReference<?>(var fqn, _) -> new TypeReference<>(fqn);
			default -> reference;
		};
	}

	private String computeErasure(ExecutableDecl executable, UnaryOperator<ITypeReference> eraser) {
		StringBuilder sb = new StringBuilder(100);
		sb.append(executable.getSimpleName());
		sb.append('(');
		for (int i = 0; i < executable.getParameters().size(); i++) {
			ParameterDecl p = executable.getParameters().get(i);
			sb.append(eraser.apply(p.type()).getQualifiedName());
			if (p.isVarargs()) {
				sb.append("[]");
			}
			if (i < executable.getParameters().size() - 1) {
				sb.append(',');
			}
		}
		sb.append(')');
		return sb.toString();
	}

	/**
	 * Returns the erased form of a reference within {@code scope}, falling back to {@code owner}'s scope for the type
	 * variables {@code scope} does not declare.
	 */
	private ITypeReference getErasedType(TypeDecl owner, TypeParameterScope scope, ITypeReference reference) {
		return switch (reference) {
			case TypeParameterReference tpr when typeParameter().resolveTypeParameter(scope, tpr).isEmpty() ->
				typeParameter().resolveTypeParameterBound(owner, tpr);
			case ArrayTypeReference(var t, var dimension) ->
				new ArrayTypeReference(getErasedType(owner, scope, t), dimension);
			default -> getErasedType(scope, reference);
		};
	}

	private String getErasureIn(ExecutableDecl executable, ExecutableDecl other) {
		return resolver().resolve(other.getContainingType())
			.map(owner -> getErasure(owner, executable))
			.orElseGet(() -> getErasure(executable));
	}
}
