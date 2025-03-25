package io.github.alien.roseau.combinatorial.v2.breaker;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;

public abstract class AbstractApiBreakerStrategy {
	protected static final Logger LOGGER = LogManager.getLogger(AbstractApiBreakerStrategy.class);

	private final NewApiQueue queue;
	private final String strategyName;
	private final String symbolName;

	public AbstractApiBreakerStrategy(NewApiQueue queue, String strategyName, Symbol symbolImpacted) {
		this.queue = queue;
		this.strategyName = strategyName;
		this.symbolName = symbolImpacted.getSimpleName();
	}

	public void breakApi(API api) {
		try {
			var mutableApi = ApiBuilder.from(api);

			applyBreakToMutableApi(mutableApi);

			queue.put(strategyName, new Pair<>(symbolName, mutableApi.make()));
		} catch (ImpossibleChangeException ignored) {
		} catch (Exception e) {
			LOGGER.error("Failed to apply breaking changes for strategy {}", strategyName);
			LOGGER.error(e.getMessage());
		}
	}

	protected abstract void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException;
}
