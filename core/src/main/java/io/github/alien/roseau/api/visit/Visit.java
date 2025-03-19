package io.github.alien.roseau.api.visit;

import io.github.alien.roseau.api.model.API;

/**
 * A default lambda type for {@link APIAlgebra} that returns no value and simply visits the {@link API}.
 */
@FunctionalInterface
public interface Visit {
	void visit();
}
