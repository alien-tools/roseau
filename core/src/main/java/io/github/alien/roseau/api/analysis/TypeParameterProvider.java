package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Optional;

/**
 * Resolves formal type parameters and their bounds in member/type scopes.
 */
public interface TypeParameterProvider {
	// Dependencies
	TypeResolver resolver();

	/**
	 * Attempts to resolve the {@link FormalTypeParameter} declared in this scope and pointed by the supplied
	 * {@link TypeParameterReference}.
	 *
	 * @param scope     the scope defining the resolution context
	 * @param reference the {@link TypeParameterReference} to resolve
	 * @return an {@link Optional} indicating whether the referenced {@link FormalTypeParameter} was found
	 */
	default Optional<FormalTypeParameter> resolveTypeParameter(TypeParameterScope scope,
	                                                           TypeParameterReference reference) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(reference);
		// Scopes are searched from the innermost out, so that a method's type parameters shadow those its enclosing
		// types declare under the same name. Searching them in place rather than collecting them first matters: this
		// sits under every type variable resolution, hence under every erasure the analysis computes
		String name = reference.getQualifiedName();
		for (Optional<TypeParameterScope> current = Optional.of(scope); current.isPresent();
		     current = enclosingScope(current.get())) {
			for (FormalTypeParameter parameter : current.get().getFormalTypeParameters()) {
				if (parameter.name().equals(name)) {
					return Optional.of(parameter);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<TypeParameterScope> enclosingScope(TypeParameterScope scope) {
		return scope.getEnclosingType().flatMap(resolver()::resolve).map(TypeParameterScope.class::cast);
	}

	/**
	 * Resolves the left-most bound of the supplied {@link TypeParameterReference}. Bounds are resolved recursively (e.g.
	 * {@code <A extends B>}) within the executable's scope.
	 *
	 * @param scope     the scope defining the resolution context
	 * @param reference the {@link TypeParameterReference} to resolve
	 * @return the resolved bound, or {@link TypeReference#OBJECT} if it was not found
	 */
	default ITypeReference resolveTypeParameterBound(TypeParameterScope scope, TypeParameterReference reference) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(reference);
		ITypeReference bound = resolveDirectTypeParameterBound(scope, reference);
		if (bound instanceof TypeParameterReference tpr) {
			return resolveTypeParameterBound(scope, tpr);
		}
		return bound;
	}

	/**
	 * Resolves only the left-most bound of the supplied type parameter (non-recursive).
	 *
	 * @param scope     the scope defining the resolution context
	 * @param reference the {@link TypeParameterReference} to resolve
	 * @return the resolved bound, or {@link TypeReference#OBJECT} if it was not found
	 */
	default ITypeReference resolveDirectTypeParameterBound(TypeParameterScope scope, TypeParameterReference reference) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(reference);
		return resolveTypeParameter(scope, reference)
			.map(tp -> tp.bounds().getFirst())
			.orElse(TypeReference.OBJECT);
	}

	/**
	 * Resolves a reference if it is a type variable, otherwise returns it unchanged.
	 *
	 * @param scope     the scope defining the resolution context
	 * @param reference the reference to resolve
	 * @return the resolved reference
	 */
	default ITypeReference resolveBound(TypeParameterScope scope, ITypeReference reference) {
		Preconditions.checkNotNull(scope);
		Preconditions.checkNotNull(reference);
		return reference instanceof TypeParameterReference tpr
			? resolveTypeParameterBound(scope, tpr)
			: reference;
	}

}
