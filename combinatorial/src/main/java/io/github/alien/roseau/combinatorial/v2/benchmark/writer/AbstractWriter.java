package io.github.alien.roseau.combinatorial.v2.benchmark.writer;

import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.combinatorial.v2.queue.AbstractQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.nio.file.Path;

public sealed abstract class AbstractWriter<T> implements Runnable permits FailedStrategiesWriter, ImpossibleStrategiesWriter, ResultsWriter {
	protected static final Logger LOGGER = LogManager.getLogger(AbstractWriter.class);

	private final Path filePath;
	private final AbstractQueue<T> queue;

	protected FileWriter fileWriter = null;

	protected boolean isBenchmarkStillOngoing = true;

	protected AbstractWriter(Path filePath, AbstractQueue<T> queue) {
		this.filePath = filePath;
		this.queue = queue;
	}

	@Override
	public void run() {
		while (isBenchmarkStillOngoing || queue.hasStillWork()) {
			var strategyAndData = queue.poll();
			if (strategyAndData == null) continue;

			var strategy = strategyAndData.getValue0();
			var data = strategyAndData.getValue1();

			addToFile(strategy, data);
		}

		closeFile();
	}

	public void informNoMoreBenchmark() {
		isBenchmarkStillOngoing = false;
	}

	protected abstract void addToFile(String strategy, T data);

	protected boolean initializeFile(String fileName, String header) {
		if (!ExplorerUtils.createDirectoryIfNecessary(filePath))
			return true;

		try {
			var now = System.currentTimeMillis();
			fileWriter = new FileWriter(filePath.resolve("%s-%d.csv".formatted(fileName, now)).toFile());
			fileWriter.write("%s\n".formatted(header));

			return false;
		} catch (Exception e) {
			LOGGER.error("Error while creating file");
			return true;
		}
	}

	private void closeFile() {
		LOGGER.info("----- Closing file -----");

		if (fileWriter != null) {
			try {
				fileWriter.close();
			} catch (Exception e) {
				LOGGER.error("Error while closing file");
			}
		}
	}
}
