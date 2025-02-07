package com.github.maracas.roseau.combinatorial.v2.benchmark.result;

import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.utils.ExplorerUtils;
import com.github.maracas.roseau.combinatorial.v2.queue.ResultsProcessQueue;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ResultsWriter implements Runnable {
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
			var groundTruth = strategyAndResults.getValue1().getValue0();
			var results = strategyAndResults.getValue1().getValue1();

			addResultsToFile(strategy, groundTruth, results);
		}

		closeResultsFile();
	}

	public void informNoMoreResults() {
		isStillProducingResults = false;
	}

	private void addResultsToFile(String strategy, boolean groundTruth, List<ToolResult> results) {
		if (resultsFileWriter == null)
			if (!createResultsFile(results))
				return;

		try {
			var resultsSortedByToolNames = results.stream()
					.sorted(Comparator.comparing(ToolResult::toolName))
					.map(ToolResult::isBreaking)
					.map(ResultsWriter::formatResult);

			resultsFileWriter.write("%s,%s,%s\n".formatted(
					strategy,
					formatResult(groundTruth),
					resultsSortedByToolNames.collect(Collectors.joining(","))
			));
		} catch (Exception e) {
			System.out.println("Error while adding results to file");
		}
	}

	private boolean createResultsFile(List<ToolResult> results) {
		if (!ExplorerUtils.createDirectoryIfNecessary(resultsPath))
			return false;

		try {
			var now = System.currentTimeMillis();
			resultsFileWriter = new FileWriter(resultsPath.resolve("results-%d.csv".formatted(now)).toFile());

			var tools = results.stream().map(ToolResult::toolName).sorted().toList();
			resultsFileWriter.write("Strategy,Ground Truth," + String.join(",", tools) + "\n");

			return true;
		} catch (Exception e) {
			System.out.println("Error while creating results file");
			return false;
		}
	}

	private void closeResultsFile() {
		if (resultsFileWriter != null) {
			try {
				resultsFileWriter.close();
			} catch (Exception e) {
				System.out.println("Error while closing results file");
			}
		}
	}

	private static String formatResult(boolean isBreaking) {
		return isBreaking ? "1" : "0";
	}
}
