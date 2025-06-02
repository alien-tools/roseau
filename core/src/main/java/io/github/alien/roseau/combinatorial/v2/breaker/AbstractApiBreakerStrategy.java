package io.github.alien.roseau.combinatorial.v2.breaker;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.ImpossibleStrategyQueue;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractApiBreakerStrategy {
	protected static final Logger LOGGER = LogManager.getLogger(AbstractApiBreakerStrategy.class);

	private final ImpossibleStrategyQueue impossibleStrategyQueue = ImpossibleStrategyQueue.getInstance();
	private final NewApiQueue newApiQueue;

	private final String strategyName;

	protected final API api;

	public AbstractApiBreakerStrategy(NewApiQueue newApiQueue, String strategyName, API api) {
		this.newApiQueue = newApiQueue;
		this.strategyName = strategyName;
		this.api = api;
	}

	public void breakApi() {
		try {
			var mutableApi = ApiBuilder.from(api);

			applyBreakToMutableApi(mutableApi);

			newApiQueue.put(strategyName, mutableApi.make());
		} catch (ImpossibleChangeException ignored) {
			impossibleStrategyQueue.put(strategyName, null);
		} catch (Exception e) {
			LOGGER.error("Failed to apply breaking changes for strategy {}", strategyName);
			LOGGER.error(e.getMessage());
		}
	}

	protected abstract void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException;
}
