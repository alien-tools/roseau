package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
		return getFormalTypeParametersInScope(scope).stream()
			.filter(tp -> tp.name().equals(reference.getQualifiedName()))
			.findFirst();
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

	private List<FormalTypeParameter> getFormalTypeParametersInScope(TypeParameterScope scope) {
		List<FormalTypeParameter> enclosingParameters = scope.getEnclosingType()
			.flatMap(resolver()::resolve)
			.map(this::getFormalTypeParametersInScope)
			.orElse(Collections.emptyList());

		if (scope instanceof ExecutableDecl executable) {
			// Method/constructor type parameters shadow enclosing type parameters with the same name.
			return Stream.concat(executable.getFormalTypeParameters().stream(), enclosingParameters.stream()).toList();
		}

		return Stream.concat(scope.getFormalTypeParameters().stream(), enclosingParameters.stream()).toList();
	}
}
