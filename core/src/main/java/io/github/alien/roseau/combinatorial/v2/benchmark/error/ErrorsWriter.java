package io.github.alien.roseau.combinatorial.v2.benchmark.error;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.combinatorial.v2.queue.FailedStrategyQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.nio.file.Path;

public final class ErrorsWriter implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(ErrorsWriter.class);

	private final Path errorsPath = Constants.ERRORS_PATH;
	private final FailedStrategyQueue failedStrategyQueue;

	private FileWriter errorsFileWriter = null;

	private boolean isBenchmarkStillOngoing = true;

	public ErrorsWriter(FailedStrategyQueue failedStrategyQueue) {
		this.failedStrategyQueue = failedStrategyQueue;
	}

	@Override
	public void run() {
		while (isBenchmarkStillOngoing || failedStrategyQueue.hasStillWork()) {
			var strategyAndError = failedStrategyQueue.poll();
			if (strategyAndError == null) continue;

			var strategy = strategyAndError.getValue0();
			var error = strategyAndError.getValue1();

			addErrorToFile(strategy, error);
		}

		closeErrorsFile();
	}

	public void informNoMoreBenchmark() {
		isBenchmarkStillOngoing = false;
	}

	private void addErrorToFile(String strategy, String error) {
		if (errorsFileWriter == null)
			if (!createErrorsFile())
				return;

		try {
			errorsFileWriter.write("%s,%s\n".formatted(strategy, error));
		} catch (Exception e) {
			LOGGER.error("Error while adding error to file");
			LOGGER.error(e);
		}
	}

	private boolean createErrorsFile() {
		if (!ExplorerUtils.createDirectoryIfNecessary(errorsPath))
			return false;

		try {
			var now = System.currentTimeMillis();
			errorsFileWriter = new FileWriter(errorsPath.resolve("errors-%d.csv".formatted(now)).toFile());

			errorsFileWriter.write("Strategy,Error\n");

			return true;
		} catch (Exception e) {
			LOGGER.error("Error while creating errors file");
			return false;
		}
	}

	private void closeErrorsFile() {
		LOGGER.info("----- Closing errors file -----");

		if (errorsFileWriter != null) {
			try {
				errorsFileWriter.close();
			} catch (Exception e) {
				LOGGER.error("Error while closing errors file");
			}
		}
	}
}
