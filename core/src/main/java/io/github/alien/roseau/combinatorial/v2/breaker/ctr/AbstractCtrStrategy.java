package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractCtrStrategy extends AbstractTpMbrStrategy {
	protected final ConstructorDecl ctr;

	AbstractCtrStrategy(ConstructorDecl ctr, NewApiQueue queue, String strategyName) {
		super(ctr, queue, strategyName);

		this.ctr = ctr;
	}
}
