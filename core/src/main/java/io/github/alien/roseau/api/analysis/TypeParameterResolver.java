package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface TypeParameterResolver {
	// Dependencies
	TypeResolver resolver();

	/**
	 * Attempts to resolve the {@link FormalTypeParameter} declared in this executable's scope and pointed by the supplied
	 * {@link TypeParameterReference}.
	 *
	 * @param executable the executable defining the resolution's scope
	 * @param reference  the {@link TypeParameterReference} to resolve
	 * @return an {@link Optional} indicating whether the referenced {@link FormalTypeParameter} was found
	 */
	default Optional<FormalTypeParameter> resolveTypeParameter(ExecutableDecl executable, TypeParameterReference reference) {
		Preconditions.checkNotNull(executable);
		Preconditions.checkNotNull(reference);
		return getFormalTypeParametersInScope(executable).stream()
			.filter(tp -> tp.name().equals(reference.getQualifiedName()))
			.findFirst();
	}

	/**
	 * Resolves the left-most bound of the supplied {@link TypeParameterReference}. Bounds are resolved recursively (e.g.
	 * {@code <A extends B>}) within the executable's scope.
	 *
	 * @param executable the executable defining the resolution's scope
	 * @param reference  the {@link TypeParameterReference} to resolve
	 * @return the resolved bound, or {@link TypeReference#OBJECT} if it was not found
	 */
	default ITypeReference resolveTypeParameterBound(ExecutableDecl executable, TypeParameterReference reference) {
		Preconditions.checkNotNull(executable);
		Preconditions.checkNotNull(reference);
		Optional<FormalTypeParameter> resolved = resolveTypeParameter(executable, reference);
		if (resolved.isPresent()) {
			ITypeReference bound = resolved.get().bounds().getFirst();
			if (bound instanceof TypeParameterReference tpr) {
				return resolveTypeParameterBound(executable, tpr);
			} else {
				return bound;
			}
		} else {
			return TypeReference.OBJECT;
		}
	}

	default ITypeReference resolveBound(ExecutableDecl executable, ITypeReference reference) {
		Preconditions.checkNotNull(executable);
		Preconditions.checkNotNull(reference);
		return reference instanceof TypeParameterReference tpr
			? resolveTypeParameterBound(executable, tpr)
			: reference;
	}

	private List<FormalTypeParameter> getFormalTypeParametersInScope(TypeDecl type) {
		return Stream.concat(
			type.getFormalTypeParameters().stream(),
			type.getEnclosingType().flatMap(resolver()::resolve)
				.map(this::getFormalTypeParametersInScope)
				.orElse(Collections.emptyList())
				.stream()
		).toList();
	}

	private List<FormalTypeParameter> getFormalTypeParametersInScope(ExecutableDecl executable) {
		return Stream.concat(
			executable.getFormalTypeParameters().stream(),
			resolver().resolve(executable.getContainingType())
				.map(this::getFormalTypeParametersInScope)
				.orElse(Collections.emptyList())
				.stream()
		).toList();
	}
}
