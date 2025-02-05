package com.github.maracas.roseau.combinatorial;

import com.github.maracas.roseau.combinatorial.api.GenerateCombinatorialApi;
import com.github.maracas.roseau.combinatorial.client.GenerateApiClients;
import com.github.maracas.roseau.combinatorial.v2.GenerateNewVersionsAndLaunchBenchmark;

import java.nio.file.Path;

public final class CombinatorialBenchmark {
	public static void main(String[] args) {
		var maxParallelAnalysis = 1;
		try {
			int firstArgParsed = args.length >= 1 ? Integer.parseInt(args[0]) : maxParallelAnalysis;
			maxParallelAnalysis = Math.max(1, firstArgParsed);
		} catch (NumberFormatException ignored) {}

		String outputDir = args.length >= 2 ? args[1] : Constants.OUTPUT_FOLDER;
		var outputPath = Path.of(outputDir);

		try {
			var apiGeneration = new GenerateCombinatorialApi(outputPath);
			apiGeneration.run();
			var api = apiGeneration.getApi();

			var clientsGeneration = new GenerateApiClients(api, outputPath);
			clientsGeneration.run();

			var newVersionsAndBenchmarkStep = new GenerateNewVersionsAndLaunchBenchmark(api, maxParallelAnalysis, outputPath);
			newVersionsAndBenchmarkStep.run();
		} catch (Exception e) {
			System.err.println("Failed to run combinatorial benchmark");
			System.err.println(e.getMessage());

			System.exit(1);
		}
	}
}
