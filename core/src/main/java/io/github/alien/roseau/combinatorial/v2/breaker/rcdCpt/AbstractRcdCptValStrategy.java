package io.github.alien.roseau.combinatorial.v2.breaker.rcdCpt;

import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractRcdCptValStrategy extends AbstractApiBreakerStrategy {
	protected final RecordComponentDecl rcdCpt;

	AbstractRcdCptValStrategy(RecordComponentDecl rcdCpt, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.rcdCpt = rcdCpt;
	}
}
