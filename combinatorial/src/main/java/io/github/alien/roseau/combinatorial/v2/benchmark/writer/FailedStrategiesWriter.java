package io.github.alien.roseau.combinatorial.v2.benchmark.writer;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.v2.queue.FailedStrategyQueue;

import java.nio.file.Path;

public final class FailedStrategiesWriter extends AbstractWriter<String> {
	public FailedStrategiesWriter(Path outputPath) {
		super(Constants.getFailedStrategiesPath(outputPath), FailedStrategyQueue.getInstance());

		LOGGER.info("Creating FailedStrategiesWriter");
	}

	@Override
	protected void addToFile(String strategy, String error) {
		if (fileWriter == null)
			if (initializeFile(filePath.getFileName().toString(), "Strategy,Error"))
				return;

		try {
			fileWriter.write("%s,%s\n".formatted(strategy, error));
		} catch (Exception e) {
			LOGGER.error("Error while adding error to file");
			LOGGER.error(e);
		}
	}
}
