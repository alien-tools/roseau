package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractCtrStrategy extends AbstractApiBreakerStrategy {
	protected final ConstructorDecl ctr;

	AbstractCtrStrategy(ConstructorDecl ctr, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.ctr = ctr;
	}
}
