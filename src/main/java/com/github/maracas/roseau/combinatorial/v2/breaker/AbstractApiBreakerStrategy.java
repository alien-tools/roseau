package com.github.maracas.roseau.combinatorial.v2.breaker;

import com.github.maracas.roseau.api.SpoonAPIFactory;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.builder.ApiBuilder;
import com.github.maracas.roseau.combinatorial.v2.queue.NewApiQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public abstract class AbstractApiBreakerStrategy {
	protected static final Logger LOGGER = LogManager.getLogger();

	private final NewApiQueue queue;
	private final String strategyName;

	private final SpoonAPIFactory factory = new SpoonAPIFactory();

	public AbstractApiBreakerStrategy(NewApiQueue queue, String strategyName) {
		this.queue = queue;
		this.strategyName = strategyName;
	}

	public void breakApi(Path apiExportPath) {
		try {
			var api = API.fromJson(apiExportPath, factory);

			var mutableApi = ApiBuilder.from(api, factory);
			applyBreakToMutableApi(api, mutableApi);

			queue.put(strategyName, mutableApi.make());
		} catch (IOException e) {
			throw new RuntimeException("Failed to break API", e);
		}
	}

	protected abstract void applyBreakToMutableApi(API api, ApiBuilder mutableApi);
}
