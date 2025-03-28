package io.github.alien.roseau.combinatorial.v2.breaker.rcdCpt;

import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractRcdCptValStrategy extends AbstractTpMbrStrategy {
	protected final RecordComponentDecl rcdCpt;

	AbstractRcdCptValStrategy(RecordComponentDecl rcdCpt, NewApiQueue queue, String strategyName) {
		super(rcdCpt, queue, strategyName);

		this.rcdCpt = rcdCpt;
	}
}
