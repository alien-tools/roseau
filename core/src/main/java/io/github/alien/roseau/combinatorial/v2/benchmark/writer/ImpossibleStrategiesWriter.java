package io.github.alien.roseau.combinatorial.v2.benchmark.writer;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.v2.queue.ImpossibleStrategyQueue;

public final class ImpossibleStrategiesWriter extends AbstractWriter<String> {
	public ImpossibleStrategiesWriter() {
		super(Constants.IMPOSSIBLE_STRATEGIES_PATH, ImpossibleStrategyQueue.getInstance());
	}

	@Override
	protected void addToFile(String strategy, String data) {
		if (fileWriter == null)
			if (initializeFile(Constants.IMPOSSIBLE_STRATEGIES_PATH.getFileName().toString(), "Strategy"))
				return;

		try {
			fileWriter.write("%s\n".formatted(strategy));
		} catch (Exception e) {
			LOGGER.error("Error while adding error to file");
			LOGGER.error(e);
		}
	}
}
