package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractRcdStrategy extends AbstractApiBreakerStrategy {
	protected final RecordDecl rcd;

	AbstractRcdStrategy(RecordDecl rcd, NewApiQueue queue, String strategyName) {
		super(queue, strategyName, rcd);

		this.rcd = rcd;
	}
}
