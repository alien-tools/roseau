package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.v2.benchmark.Benchmark;
import spoon.SpoonException;

import java.nio.file.Path;

public class V2Generator {
	public static void main(String[] args) {
		var maxParallelAnalysis = 1;
		try {
			int firstArgParsed = args.length >= 1 ? Integer.parseInt(args[0]) : maxParallelAnalysis;
			maxParallelAnalysis = Math.max(1, firstArgParsed);
		} catch (NumberFormatException ignored) {}

		String v1SourcesDir = args.length >= 2 ? args[1] : Constants.DEFAULT_API_DIR;
		String clientsSourcesDir = args.length >= 3 ? args[2] : Constants.DEFAULT_CLIENTS_DIR;
		String workingDir = args.length >= 4 ? args[3] : Constants.DEFAULT_WORKING_DIR;

		var v1SourcesPath = checkDirectory(v1SourcesDir);
		var clientsSourcesPath = checkDirectory(clientsSourcesDir);

		// TODO: Package V1 API & Clients separately to working directory
		var v1JarPath = Path.of("");
		var clientsJarPath = Path.of("");

		var newApiQueue = new NewApiQueue(maxParallelAnalysis);

		for (int i = 0; i < maxParallelAnalysis; i++) {
			var benchmark = new Benchmark(String.valueOf(i), newApiQueue, clientsSourcesPath, clientsJarPath, v1SourcesPath, v1JarPath, workingDir);
			new Thread(benchmark).start();
		}
		System.out.println("---- All benchmark threads started ---\n");

		try {
			APIExtractor apiExtractor = new SpoonAPIExtractor();
			var generatedV1Api = apiExtractor.extractAPI(v1SourcesPath);

			var visitor = new BreakingChangesGeneratorVisitor(generatedV1Api, newApiQueue);
			visitor.$(generatedV1Api).visit();
		} catch (SpoonException e) {
			System.err.println("Failed to extract API from " + v1SourcesDir);
			System.err.println(e.getMessage());

			System.exit(1);
		} catch (Exception e) {
			System.err.println(e.getMessage());

			System.exit(1);
		}
	}

	private static Path checkDirectory(String dir) {
		var path = Path.of(dir);

		if (!path.toFile().exists()) {
			System.err.println("Directory " + dir + " does not exist");
			System.exit(1);
		}

		return path;
	}
}
