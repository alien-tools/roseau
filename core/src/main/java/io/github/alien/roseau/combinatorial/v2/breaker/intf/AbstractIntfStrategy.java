package io.github.alien.roseau.combinatorial.v2.breaker.intf;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractIntfStrategy extends AbstractApiBreakerStrategy {
	protected final InterfaceDecl intf;

	AbstractIntfStrategy(InterfaceDecl intf, NewApiQueue queue, String strategyName) {
		super(queue, strategyName, intf);

		this.intf = intf;
	}
}
