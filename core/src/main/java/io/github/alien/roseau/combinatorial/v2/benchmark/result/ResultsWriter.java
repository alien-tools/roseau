package io.github.alien.roseau.combinatorial.v2.benchmark.result;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.combinatorial.v2.queue.ResultsProcessQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ResultsWriter implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(ResultsWriter.class);

	private final Path resultsPath = Path.of(Constants.RESULTS_FOLDER);
	private final ResultsProcessQueue resultsQueue;

	private FileWriter resultsFileWriter = null;

	private boolean isStillProducingResults = true;

	public ResultsWriter(ResultsProcessQueue resultsQueue) {
		this.resultsQueue = resultsQueue;
	}

	@Override
	public void run() {
		while (isStillProducingResults || resultsQueue.hasStillWork()) {
			var strategyAndResults = resultsQueue.poll();
			if (strategyAndResults == null) continue;

			var strategy = strategyAndResults.getValue0();
			var results = strategyAndResults.getValue1();

			addResultsToFile(strategy, results);
		}

		closeResultsFile();
	}

	public void informNoMoreResults() {
		isStillProducingResults = false;
	}

	private void addResultsToFile(String strategy, List<ToolResult> results) {
		var sortedResults = results.stream().sorted(Comparator.comparing(ToolResult::toolName)).toList();
		if (resultsFileWriter == null)
			if (!createResultsFile(sortedResults))
				return;

		try {
			var formatedResults = sortedResults.stream().map(ResultsWriter::formatResult);

			resultsFileWriter.write("%s,%s\n".formatted(
					strategy,
					formatedResults.collect(Collectors.joining(","))
			));
		} catch (Exception e) {
			LOGGER.error("Error while adding results to file");
			LOGGER.error(e);
		}
	}

	private boolean createResultsFile(List<ToolResult> results) {
		if (!ExplorerUtils.createDirectoryIfNecessary(resultsPath))
			return false;

		try {
			var now = System.currentTimeMillis();
			resultsFileWriter = new FileWriter(resultsPath.resolve("results-%d.csv".formatted(now)).toFile());

			var toolNames = results.stream().map(t -> "%s Binary,%s Source".formatted(t.toolName(), t.toolName())).toList();
			resultsFileWriter.write("Strategy," + String.join(",", toolNames) + "\n");

			return true;
		} catch (Exception e) {
			LOGGER.error("Error while creating results file");
			return false;
		}
	}

	private void closeResultsFile() {
		LOGGER.info("----- Closing results file -----");

		if (resultsFileWriter != null) {
			try {
				resultsFileWriter.close();
			} catch (Exception e) {
				LOGGER.error("Error while closing results file");
			}
		}
	}

	private static String formatResult(ToolResult result) {
		return "%s,%s".formatted(
				result.isBinaryBreaking() ? "1" : "0",
				result.isSourceBreaking() ? "1" : "0"
		);
	}
}
