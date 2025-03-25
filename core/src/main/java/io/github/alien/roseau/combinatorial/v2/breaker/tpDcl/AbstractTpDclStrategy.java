package io.github.alien.roseau.combinatorial.v2.breaker.tpDcl;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractTpDclStrategy extends AbstractApiBreakerStrategy {
	protected final TypeDecl tpDcl;

	AbstractTpDclStrategy(TypeDecl tpDcl, NewApiQueue queue, String strategyName) {
		super(queue, strategyName, tpDcl);

		this.tpDcl = tpDcl;
	}
}
