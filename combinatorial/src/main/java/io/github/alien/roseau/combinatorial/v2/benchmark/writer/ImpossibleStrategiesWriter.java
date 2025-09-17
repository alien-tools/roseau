package io.github.alien.roseau.combinatorial.v2.benchmark.writer;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.v2.queue.ImpossibleStrategyQueue;

import java.nio.file.Path;

public final class ImpossibleStrategiesWriter extends AbstractWriter<String> {
	public ImpossibleStrategiesWriter(Path outputPath) {
		super(Constants.getImpossibleStrategiesPath(outputPath), ImpossibleStrategyQueue.getInstance());

		LOGGER.info("Creating ImpossibleStrategiesWriter");
	}

	@Override
	protected void addToFile(String strategy, String data) {
		if (fileWriter == null)
			if (initializeFile(filePath.getFileName().toString(), "Strategy"))
				return;

		try {
			fileWriter.write("%s\n".formatted(strategy));
		} catch (Exception e) {
			LOGGER.error("Error while adding error to file");
			LOGGER.error(e);
		}
	}
}
