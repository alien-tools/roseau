package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.v2.benchmark.Benchmark;
import com.github.maracas.roseau.combinatorial.v2.compiler.InternalJavaCompiler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GenerateNewVersionsAndLaunchBenchmark extends AbstractStep {
	private final API v1Api;
	private final int maxParallelAnalysis;

	private final NewApiQueue newApiQueue;

	private final List<Benchmark> benchmarks = new ArrayList<>();

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	private final Path v1SourcesPath;
	private final Path clientsSourcesPath;
	private final Path benchmarkTempPath;
	private final Path v1JarPath;
	private final Path clientsBinPath;

	public GenerateNewVersionsAndLaunchBenchmark(API v1Api, int maxParallelAnalysis, Path outputPath) {
		super(outputPath);

		this.v1Api = v1Api;
		this.maxParallelAnalysis = maxParallelAnalysis;

		newApiQueue = new NewApiQueue(maxParallelAnalysis);

		v1SourcesPath = outputPath.resolve(Constants.API_FOLDER);
		clientsSourcesPath = outputPath.resolve(Constants.CLIENTS_FOLDER);
		benchmarkTempPath = Path.of(Constants.BENCHMARK_TMP_FOLDER);
		v1JarPath = benchmarkTempPath.resolve(Path.of(Constants.JAR_FOLDER, Constants.API_FOLDER));
		clientsBinPath = benchmarkTempPath.resolve(Path.of(Constants.BINARIES_FOLDER, Constants.CLIENTS_FOLDER));
	}

	public void run() {
		checkPath(v1SourcesPath);
		checkPath(clientsSourcesPath);

		packageV1Api();
		compileClients();

		initializeBenchmarkThreads();

		try {
			var visitor = new BreakingChangesGeneratorVisitor(v1Api, newApiQueue);
			visitor.$(v1Api).visit();

			informAllBenchmarksGenerationIsOver();
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}

	private void packageV1Api() {

	}

	private void compileClients() {

	}

	private void initializeBenchmarkThreads() {
		System.out.println("\n----- Starting benchmark threads -----");

		for (int i = 0; i < maxParallelAnalysis; i++) {
			var benchmark = new Benchmark(String.valueOf(i), newApiQueue, clientsSourcesPath, clientsBinPath, v1SourcesPath, v1JarPath, benchmarkTempPath);

			benchmarks.add(benchmark);
			new Thread(benchmark).start();
		}

		System.out.println("---- All benchmark threads started ---\n");
	}

	private void informAllBenchmarksGenerationIsOver() {
		for (Benchmark benchmark : benchmarks)
			benchmark.informsBreakingApisGenerationIsOver();
	}
}
