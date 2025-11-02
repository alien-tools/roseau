package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.LibraryTypes;

/**
 * A default lambda type for {@link ApiAlgebra} that returns no value and simply visits the {@link LibraryTypes}.
 */
@FunctionalInterface
public interface Visit {
	void visit();
}
