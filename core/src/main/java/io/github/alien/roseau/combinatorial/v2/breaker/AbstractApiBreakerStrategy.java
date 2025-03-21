package io.github.alien.roseau.combinatorial.v2.breaker;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.reference.CachedTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;
import io.github.alien.roseau.extractors.spoon.SpoonAPIFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public abstract class AbstractApiBreakerStrategy {
	protected static final Logger LOGGER = LogManager.getLogger();

	private final NewApiQueue queue;
	private final String strategyName;

	private final TypeReferenceFactory typeReferenceFactory = new CachedTypeReferenceFactory();
	private final SpoonAPIFactory factory = new SpoonAPIFactory(typeReferenceFactory);

	public AbstractApiBreakerStrategy(NewApiQueue queue, String strategyName) {
		this.queue = queue;
		this.strategyName = strategyName;
	}

	public void breakApi(Path apiExportPath) {
		try {
			var api = API.fromJson(apiExportPath, typeReferenceFactory);

			var mutableApi = ApiBuilder.from(api, factory);
			applyBreakToMutableApi(api, mutableApi);

			queue.put(strategyName, mutableApi.make());
		} catch (IOException e) {
			throw new RuntimeException("Failed to break API", e);
		}
	}

	protected abstract void applyBreakToMutableApi(API api, ApiBuilder mutableApi);
}
