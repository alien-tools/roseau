package io.github.alien.roseau.combinatorial.v2;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.combinatorial.v2.benchmark.Benchmark;
import io.github.alien.roseau.combinatorial.v2.benchmark.result.ResultsWriter;
import io.github.alien.roseau.combinatorial.v2.compiler.InternalJavaCompiler;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;
import io.github.alien.roseau.combinatorial.v2.queue.ResultsProcessQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GenerateNewVersionsAndLaunchBenchmark extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger(GenerateNewVersionsAndLaunchBenchmark.class);

	private final API v1Api;
	private final int maxParallelAnalysis;

	private final NewApiQueue newApiQueue;
	private final ResultsProcessQueue resultsQueue;

	private final Map<Benchmark, Thread> benchmarkThreads = new HashMap<>();
	private ResultsWriter resultsWriter = null;

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	private final Path tmpPath = Path.of(Constants.TMP_FOLDER);
	private final Path v1SourcesPath;
	private final Path clientsSourcesPath;
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
		v1JarPath = tmpPath.resolve(Path.of(Constants.JAR_FOLDER, "v1.jar"));
		clientsBinPath = tmpPath.resolve(Constants.BINARIES_FOLDER);

		ExplorerUtils.cleanOrCreateDirectory(tmpPath);
	}

	public void run() throws StepExecutionException {
		checkSourcesArePresent();

		packageV1Api();
		compileClients();

		try {
			initializeBenchmarkThreads();
			initializeResultsThread();

			var visitor = new BreakingChangesGeneratorVisitor(v1Api, newApiQueue);
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
		LOGGER.info("------- Packaging V1 API -------");

		var errors = compiler.packageApiToJar(v1SourcesPath, v1JarPath);

		if (!errors.isEmpty())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Couldn't package V1 API: " + formatCompilerErrors(errors));

		LOGGER.info("-------- V1 API packaged -------\n");
	}

	private void compileClients() throws StepExecutionException {
		LOGGER.info("------- Compiling clients ------");

		var errors = compiler.compileClientsWithApi(clientsSourcesPath, v1JarPath, clientsBinPath);

		if (!errors.isEmpty())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Couldn't compile clients: " + formatCompilerErrors(errors));

		LOGGER.info("------- Clients compiled -------\n");
	}

	private static String formatCompilerErrors(List<?> errors) {
		return errors.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
	}

	private void initializeBenchmarkThreads() {
		LOGGER.info("-- Starting benchmark threads --");

		for (int i = 0; i < maxParallelAnalysis; i++) {
			var benchmark = new Benchmark(
					String.valueOf(i),
					newApiQueue, resultsQueue,
					clientsBinPath, clientsSourcesPath,
					v1SourcesPath, v1JarPath,
					tmpPath,
					v1Api
			);
			var thread = new Thread(benchmark);
			thread.start();

			benchmarkThreads.put(benchmark, thread);
		}

		LOGGER.info("--- All bench threads started --\n");
	}

	private void initializeResultsThread() {
		LOGGER.info("---- Starting results thread ---");

		resultsWriter = new ResultsWriter(resultsQueue);
		new Thread(resultsWriter).start();

		LOGGER.info("---- Results thread started ----\n");
	}

	private void informAllBenchmarksGenerationIsOver() {
		for (var benchmark : benchmarkThreads.keySet())
			benchmark.informApisGenerationIsOver();

		for (var thread : benchmarkThreads.values())
			try { thread.join(); } catch (InterruptedException ignored) {}

		LOGGER.info("-- All bench threads finished --");
		int totalErrors = benchmarkThreads.keySet().stream().mapToInt(Benchmark::getErrorsCount).sum();
		LOGGER.info("Total benchmark errors: " + totalErrors);

		if (totalErrors == 0)
			ExplorerUtils.removeDirectory(tmpPath);

		informResultsThreadNoMoreResults();
	}

	private void informResultsThreadNoMoreResults() {
		if (resultsWriter != null) {
			resultsWriter.informNoMoreResults();
		}
	}
}
