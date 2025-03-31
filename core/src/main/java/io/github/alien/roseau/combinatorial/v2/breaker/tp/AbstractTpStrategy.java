package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractTpStrategy<T extends TypeDecl> extends AbstractApiBreakerStrategy {
	protected final T tp;

	AbstractTpStrategy(T tp, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.tp = tp;
	}
}
