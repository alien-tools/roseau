package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.utils.ExplorerUtils;
import com.github.maracas.roseau.combinatorial.v2.benchmark.Benchmark;
import com.github.maracas.roseau.combinatorial.v2.benchmark.result.ResultsWriter;
import com.github.maracas.roseau.combinatorial.v2.queue.ResultsProcessQueue;
import com.github.maracas.roseau.combinatorial.v2.compiler.InternalJavaCompiler;
import com.github.maracas.roseau.combinatorial.v2.queue.NewApiQueue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GenerateNewVersionsAndLaunchBenchmark extends AbstractStep {
	private final API v1Api;
	private final int maxParallelAnalysis;

	private final NewApiQueue newApiQueue;
	private final ResultsProcessQueue resultsQueue;

	private final Map<Benchmark, Thread> benchmarkThreads = new HashMap<>();
	private ResultsWriter resultsWriter = null;

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	private final Path v1SourcesPath;
	private final Path clientsSourcesPath;
	private final Path tmpPath;
	private final Path v1JarPath;
	private final Path clientsBinPath;

	public GenerateNewVersionsAndLaunchBenchmark(API v1Api, int maxParallelAnalysis, Path outputPath) {
		super(outputPath);

		this.v1Api = v1Api;
		this.maxParallelAnalysis = maxParallelAnalysis;

		newApiQueue = new NewApiQueue(maxParallelAnalysis);
		resultsQueue = new ResultsProcessQueue();

		v1SourcesPath = outputPath.resolve(Constants.API_FOLDER);
		clientsSourcesPath = outputPath.resolve(Constants.CLIENTS_FOLDER);
		tmpPath = Path.of(Constants.TMP_FOLDER);
		v1JarPath = tmpPath.resolve(Path.of(Constants.JAR_FOLDER, "v1.jar"));
		clientsBinPath = tmpPath.resolve(Path.of(Constants.BINARIES_FOLDER));

		ExplorerUtils.cleanOrCreateDirectory(tmpPath);
	}

	public void run() throws StepExecutionException {
		checkSourcesArePresent();

		packageV1Api();
		compileClients();

		try {
			initializeBenchmarkThreads();
			initializeResultsThread();

			var visitor = new BreakingChangesGeneratorVisitor(newApiQueue, outputPath);
			visitor.$(v1Api).visit();

			informAllBenchmarksGenerationIsOver();
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}

	private void checkSourcesArePresent() throws StepExecutionException {
		if (!ExplorerUtils.checkPathExists(v1SourcesPath))
			throw new StepExecutionException(this.getClass().getSimpleName(), "V1 API sources are missing");

		if (!ExplorerUtils.checkPathExists(clientsSourcesPath))
			throw new StepExecutionException(this.getClass().getSimpleName(), "Clients sources are missing");
	}

	private void packageV1Api() throws StepExecutionException {
		var errors = compiler.packageApiToJar(v1SourcesPath, v1JarPath);

		if (!errors.isEmpty())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Couldn't package V1 API: " + formatCompilerErrors(errors));
	}

	private void compileClients() throws StepExecutionException {
		var errors = compiler.compileClientWithApi(clientsSourcesPath, v1JarPath, clientsBinPath);

		if (!errors.isEmpty())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Couldn't compile clients: " + formatCompilerErrors(errors));
	}

	private static String formatCompilerErrors(List<?> errors) {
		return errors.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
	}

	private void initializeBenchmarkThreads() {
		System.out.println("\n-- Starting benchmark threads --");

		for (int i = 0; i < maxParallelAnalysis; i++) {
			var benchmark = new Benchmark(String.valueOf(i), newApiQueue, resultsQueue, clientsSourcesPath, v1SourcesPath, v1JarPath, tmpPath);
			var thread = new Thread(benchmark);
			thread.start();

			benchmarkThreads.put(benchmark, thread);
		}

		System.out.println("--- All bench threads started --\n");
	}

	private void initializeResultsThread() {
		System.out.println("\n---- Starting results thread ---");

		resultsWriter = new ResultsWriter(resultsQueue);
		new Thread(resultsWriter).start();

		System.out.println("---- Results thread started ----\n");
	}

	private void informAllBenchmarksGenerationIsOver() {
		for (var benchmark : benchmarkThreads.keySet())
			benchmark.informsBreakingApisGenerationIsOver();

		for (var thread : benchmarkThreads.values())
			try { thread.join(); } catch (InterruptedException ignored) {}

		System.out.println("-- All bench threads finished --");
		int totalErrors = benchmarkThreads.keySet().stream().mapToInt(Benchmark::getErrorsCount).sum();
		System.out.println("Total benchmark errors: " + totalErrors);

		if (totalErrors == 0)
			ExplorerUtils.removeDirectory(tmpPath);

		informResultsThreadNoMoreResults();
	}

	private void informResultsThreadNoMoreResults() {
		System.out.println("\n----- Closing results file -----");

		if (resultsWriter != null) {
			resultsWriter.informNoMoreResults();
		}
	}
}
