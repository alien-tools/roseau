package com.github.maracas.roseau.combinatorial.v2.breaker.intf;

import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.combinatorial.v2.queue.NewApiQueue;
import com.github.maracas.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;

abstract class AbstractIntfStrategy extends AbstractApiBreakerStrategy {
	protected final InterfaceDecl intf;

	AbstractIntfStrategy(InterfaceDecl intf, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.intf = intf;
	}
}
