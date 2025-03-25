package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractClsStrategy extends AbstractApiBreakerStrategy {
	protected final ClassDecl cls;

	AbstractClsStrategy(ClassDecl cls, NewApiQueue queue, String strategyName) {
		super(queue, strategyName, cls);

		this.cls = cls;
	}
}
