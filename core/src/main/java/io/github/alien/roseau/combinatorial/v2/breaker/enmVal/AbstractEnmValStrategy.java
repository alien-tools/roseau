package io.github.alien.roseau.combinatorial.v2.breaker.enmVal;

import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractEnmValStrategy extends AbstractTpMbrStrategy {
	protected final EnumValueDecl enmVal;

	AbstractEnmValStrategy(EnumValueDecl enmVal, NewApiQueue queue, String strategyName) {
		super(enmVal, queue, strategyName);

		this.enmVal = enmVal;
	}
}
