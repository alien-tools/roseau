package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public abstract class AbstractTpStrategy<T extends TypeDecl> extends AbstractApiBreakerStrategy {
	protected final T tp;

	public AbstractTpStrategy(T tp, NewApiQueue queue, String strategyName, API api) {
		super(queue, strategyName, api);

		this.tp = tp;
	}
}
