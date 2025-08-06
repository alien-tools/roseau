package io.github.alien.roseau.combinatorial.v2.benchmark.writer;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import io.github.alien.roseau.combinatorial.v2.queue.ResultsProcessQueue;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ResultsWriter extends AbstractWriter<List<ToolResult>> {
	public ResultsWriter(Path outputPath) {
		super(Constants.getResultsPath(outputPath), ResultsProcessQueue.getInstance());

		LOGGER.info("Creating ResultsWriter");
	}

	@Override
	protected void addToFile(String strategy, List<ToolResult> results) {
		var sortedResults = results.stream().sorted(Comparator.comparing(ToolResult::toolName)).toList();
		if (fileWriter == null) {
			var toolNames = results.stream().map(t -> "%s Binary,%s Source".formatted(t.toolName(), t.toolName())).toList();
			var header = "Strategy," + String.join(",", toolNames);

			if (initializeFile(filePath.getFileName().toString(), header))
				return;
		}

		try {
			var formatedResults = sortedResults.stream()
					.map(ResultsWriter::formatResult)
					.collect(Collectors.joining(","));

			fileWriter.write("%s,%s\n".formatted(strategy, formatedResults));
		} catch (Exception e) {
			LOGGER.error("Error while adding results to file");
			LOGGER.error(e);
		}
	}

	private static String formatResult(ToolResult result) {
		return "%s,%s".formatted(
				result.isBinaryBreaking() ? "1" : "0",
				result.isSourceBreaking() ? "1" : "0"
		);
	}
}
