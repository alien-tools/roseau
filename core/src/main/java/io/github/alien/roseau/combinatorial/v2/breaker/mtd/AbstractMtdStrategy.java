package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractMtdStrategy extends AbstractTpMbrStrategy {
	protected final MethodDecl mtd;

	AbstractMtdStrategy(MethodDecl mtd, NewApiQueue queue, String strategyName) {
		super(mtd, queue, strategyName);

		this.mtd = mtd;
	}
}
