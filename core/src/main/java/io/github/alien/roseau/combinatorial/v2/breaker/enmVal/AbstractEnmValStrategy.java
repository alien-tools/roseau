package io.github.alien.roseau.combinatorial.v2.breaker.enmVal;

import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractEnmValStrategy extends AbstractApiBreakerStrategy {
	protected final EnumValueDecl enmVal;

	AbstractEnmValStrategy(EnumValueDecl enmVal, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.enmVal = enmVal;
	}
}
