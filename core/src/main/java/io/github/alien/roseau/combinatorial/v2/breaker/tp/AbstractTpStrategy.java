package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractTpStrategy extends AbstractApiBreakerStrategy {
	protected final TypeDecl tp;

	AbstractTpStrategy(TypeDecl tp, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.tp = tp;
	}
}
