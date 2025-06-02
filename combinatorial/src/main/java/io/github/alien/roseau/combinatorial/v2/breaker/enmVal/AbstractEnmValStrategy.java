package io.github.alien.roseau.combinatorial.v2.breaker.enmVal;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractEnmValStrategy extends AbstractTpMbrStrategy<EnumValueDecl> {
	AbstractEnmValStrategy(EnumValueDecl enmVal, NewApiQueue queue, String strategyName, API api) {
		super(enmVal, queue, strategyName, api);
	}
}
