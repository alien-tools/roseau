package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractFldStrategy extends AbstractApiBreakerStrategy {
	protected final FieldDecl fld;

	AbstractFldStrategy(FieldDecl fld, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.fld = fld;
	}
}
