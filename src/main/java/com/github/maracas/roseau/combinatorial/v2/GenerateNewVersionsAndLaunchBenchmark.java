package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.v2.benchmark.Benchmark;

import java.nio.file.Path;

public final class GenerateNewVersionsAndLaunchBenchmark extends AbstractStep {
	private final API v1Api;
	private final int maxParallelAnalysis;

	public GenerateNewVersionsAndLaunchBenchmark(API v1Api, int maxParallelAnalysis, Path outputPath) {
		super(outputPath);

		this.v1Api = v1Api;
		this.maxParallelAnalysis = maxParallelAnalysis;
	}

	public void run() {
		Path v1SourcesPath = outputPath.resolve(Constants.API_FOLDER);
		Path clientsSourcesPath = outputPath.resolve(Constants.CLIENTS_FOLDER);
		Path benchmarkTempPath = Path.of(Constants.BENCHMARK_TMP_FOLDER);

		checkPath(v1SourcesPath);
		checkPath(clientsSourcesPath);

		// TODO: Package V1 API & Clients separately to working directory
		var v1JarPath = Path.of("");
		var clientsJarPath = Path.of("");

		var newApiQueue = new NewApiQueue(maxParallelAnalysis);

		for (int i = 0; i < maxParallelAnalysis; i++) {
			var benchmark = new Benchmark(String.valueOf(i), newApiQueue, clientsSourcesPath, clientsJarPath, v1SourcesPath, v1JarPath, benchmarkTempPath);
			new Thread(benchmark).start();
		}
		System.out.println("---- All benchmark threads started ---\n");

		try {
			var visitor = new BreakingChangesGeneratorVisitor(v1Api, newApiQueue);
			visitor.$(v1Api).visit();
		} catch (Exception e) {
			System.err.println(e.getMessage());

			System.exit(1);
		}
	}
}
