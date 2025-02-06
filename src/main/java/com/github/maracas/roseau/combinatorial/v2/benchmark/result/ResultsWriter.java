package com.github.maracas.roseau.combinatorial.v2.benchmark.result;

import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.v2.queue.ResultsProcessQueue;
import org.javatuples.Pair;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

public class ResultsWriter implements Runnable {
	private final Path resultsPath;
	private final ResultsProcessQueue resultsQueue;

	private FileWriter resultsFile = null;

	private boolean isStillProducingResults = true;

	public ResultsWriter(ResultsProcessQueue resultsQueue, Path outputPath) {
		this.resultsPath = outputPath.resolve(Constants.RESULTS_FOLDER);
		this.resultsQueue = resultsQueue;
	}

	@Override
	public void run() {
		while (isStillProducingResults || resultsQueue.hasStillWork()) {
			var strategyAndResults = resultsQueue.take();
			if (strategyAndResults == null) break;

			addResultsToFile(strategyAndResults);
		}

		closeResultsFile();
	}

	public void informNoMoreResults() {
		isStillProducingResults = false;
	}

	private void addResultsToFile(Pair<String, List<ToolResult>> strategyAndResults) {
		var strategy = strategyAndResults.getValue0();
		var results = strategyAndResults.getValue1();

		if (resultsFile == null)
			createResultsFile(results);

		try {
			// TODO: Write results to file
			resultsFile.write("");
		} catch (Exception e) {
			System.out.println("Error while adding results to file");
		}
	}

	private void createResultsFile(List<ToolResult> results) {
		// TODO: Create folder if it doesn't exist
		// TODO: Create new file

		var tools = results.stream().map(ToolResult::toolName).toArray();
		// TODO: Write csv header
	}

	private void closeResultsFile() {
		if (resultsFile != null) {
			try {
				resultsFile.close();
			} catch (Exception e) {
				System.out.println("Error while closing results file");
			}
		}
	}
}
