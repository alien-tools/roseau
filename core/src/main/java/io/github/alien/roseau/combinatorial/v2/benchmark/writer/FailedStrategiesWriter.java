package io.github.alien.roseau.combinatorial.v2.benchmark.writer;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.v2.queue.FailedStrategyQueue;

public final class FailedStrategiesWriter extends AbstractWriter<String> {
	public FailedStrategiesWriter() {
		super(Constants.ERRORS_PATH, FailedStrategyQueue.getInstance());
	}

	@Override
	protected void addToFile(String strategy, String error) {
		if (fileWriter == null)
			if (initializeFile(Constants.ERRORS_PATH.getFileName().toString(), "Strategy,Error"))
				return;

		try {
			fileWriter.write("%s,%s\n".formatted(strategy, error));
		} catch (Exception e) {
			LOGGER.error("Error while adding error to file");
			LOGGER.error(e);
		}
	}
}
