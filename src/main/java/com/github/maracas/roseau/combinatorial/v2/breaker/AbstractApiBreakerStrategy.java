package com.github.maracas.roseau.combinatorial.v2.breaker;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.builder.ApiBuilder;
import com.github.maracas.roseau.combinatorial.v2.NewApiQueue;

public abstract class AbstractApiBreakerStrategy {
	private final NewApiQueue queue;

	public AbstractApiBreakerStrategy(NewApiQueue queue) {
		this.queue = queue;
	}

	public void breakApi(API api) {
		var mutableApi = ApiBuilder.from(api);
		applyBreakToMutableApi(api, mutableApi);

		queue.put(mutableApi.make());
	}

	protected abstract void applyBreakToMutableApi(API api, ApiBuilder mutableApi);
}
