package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.List;
import java.util.Optional;

/**
 * A scope in which formal type parameters can be resolved.
 */
public interface TypeParameterScope {
	TypeParameterScope EMPTY = new TypeParameterScope() {
		@Override
		public List<FormalTypeParameter> getFormalTypeParameters() {
			return List.of();
		}

		@Override
		public Optional<TypeReference<TypeDecl>> getEnclosingType() {
			return Optional.empty();
		}
	};

	List<FormalTypeParameter> getFormalTypeParameters();

	Optional<TypeReference<TypeDecl>> getEnclosingType();
}
