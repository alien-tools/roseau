package com.github.maracas.roseau.combinatorial;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.api.GenerateCombinatorialApi;
import com.github.maracas.roseau.combinatorial.client.GenerateApiClients;
import com.github.maracas.roseau.combinatorial.v2.GenerateNewVersionsAndLaunchBenchmark;

import java.io.IOException;
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

			var api = getAndExportGeneratedApi(outputPath);

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

	private static API getAndExportGeneratedApi(Path outputPath) {
		var apiPath = outputPath.resolve(Constants.API_FOLDER);
		var apiExtractor = new SpoonAPIExtractor();
		var api = apiExtractor.extractAPI(apiPath);

		try {
			var exportPath = outputPath.resolve(Constants.API_JSON);
			api.writeJson(exportPath);
		} catch (IOException e) {
			throw new RuntimeException("Failed to export generated API", e);
		}

		return api;
	}
}
