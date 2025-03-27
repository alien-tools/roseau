package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractMtdStrategy extends AbstractApiBreakerStrategy {
	protected final MethodDecl mtd;

	AbstractMtdStrategy(MethodDecl mtd, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.mtd = mtd;
	}
}
