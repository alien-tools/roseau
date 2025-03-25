package io.github.alien.roseau.combinatorial.v2.breaker.enm;

import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractEnmStrategy extends AbstractApiBreakerStrategy {
	protected final EnumDecl enm;

	AbstractEnmStrategy(EnumDecl enm, NewApiQueue queue, String strategyName) {
		super(queue, strategyName, enm);

		this.enm = enm;
	}
}
