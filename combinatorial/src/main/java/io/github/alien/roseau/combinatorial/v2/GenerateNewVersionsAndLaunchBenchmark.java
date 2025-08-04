package io.github.alien.roseau.combinatorial.v2;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.compiler.InternalJavaCompiler;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.combinatorial.v2.benchmark.Benchmark;
import io.github.alien.roseau.combinatorial.v2.benchmark.writer.FailedStrategiesWriter;
import io.github.alien.roseau.combinatorial.v2.benchmark.writer.ImpossibleStrategiesWriter;
import io.github.alien.roseau.combinatorial.v2.benchmark.writer.ResultsWriter;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class GenerateNewVersionsAndLaunchBenchmark extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger(GenerateNewVersionsAndLaunchBenchmark.class);

	private final API v1Api;
	private final int maxParallelAnalysis;

	private final NewApiQueue newApiQueue;

	private final FailedStrategiesWriter failedStrategiesWriter = new FailedStrategiesWriter();
	private final ImpossibleStrategiesWriter impossibleStrategiesWriter = new ImpossibleStrategiesWriter();
	private final ResultsWriter resultsWriter = new ResultsWriter();

	private final Map<Benchmark, Thread> benchmarkThreads = new HashMap<>();

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	private final Path tmpPath;
	private final Path v1SourcesPath;
	private final Path v1JarPath;
	private final Path clientSourcePath;
	private final Path clientBinPath;

	public GenerateNewVersionsAndLaunchBenchmark(API v1Api, int maxParallelAnalysis, Path outputPath, Path tmpOutputPath) {
		super(outputPath);

		this.v1Api = v1Api;
		this.maxParallelAnalysis = maxParallelAnalysis;

		newApiQueue = new NewApiQueue(maxParallelAnalysis);

		tmpPath = tmpOutputPath;
		v1SourcesPath = outputPath.resolve(Constants.API_FOLDER);
		v1JarPath = tmpPath.resolve(Path.of(Constants.JAR_FOLDER, "v1.jar"));
		clientSourcePath = outputPath.resolve(Constants.CLIENT_FOLDER);
		clientBinPath = tmpPath.resolve(Constants.BINARIES_FOLDER);

		ExplorerUtils.cleanOrCreateDirectory(tmpPath);
	}

	public void run() throws StepExecutionException {
		try {
			compiler.checkClientCompilesWithApi(clientSourcePath, v1SourcesPath, clientBinPath, v1JarPath);

			initializeBenchmarkThreads();
			initializeWritersThreads();

			var visitor = new BreakingChangesGeneratorVisitor(v1Api, newApiQueue);
			visitor.$(v1Api).visit();

			informAllThreadsGenerationIsOver();
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}

	private void initializeBenchmarkThreads() {
		LOGGER.info("-- Starting benchmark threads --");

		for (int i = 0; i < maxParallelAnalysis; i++) {
			var benchmark = new Benchmark(
					String.valueOf(i),
					newApiQueue,
					clientBinPath, clientSourcePath,
					v1JarPath,
					tmpPath
			);
			var thread = new Thread(benchmark);
			thread.start();

			benchmarkThreads.put(benchmark, thread);
		}

		LOGGER.info("--- All bench threads started --\n");
	}

	private void initializeWritersThreads() {
		LOGGER.info("---- Starting writers threads ---");

		new Thread(failedStrategiesWriter).start();
		new Thread(impossibleStrategiesWriter).start();
		new Thread(resultsWriter).start();

		LOGGER.info("---- All writers threads started ----\n");
	}

	private void informAllThreadsGenerationIsOver() {
		for (var benchmark : benchmarkThreads.keySet())
			benchmark.informApisGenerationIsOver();

		for (var thread : benchmarkThreads.values())
			try { thread.join(); } catch (InterruptedException ignored) {}

		LOGGER.info("-- All bench threads finished --");
		int totalErrors = benchmarkThreads.keySet().stream().mapToInt(Benchmark::getErrorsCount).sum();
		LOGGER.info("Total benchmark errors: {}", totalErrors);

		failedStrategiesWriter.informNoMoreBenchmark();
		impossibleStrategiesWriter.informNoMoreBenchmark();
		resultsWriter.informNoMoreBenchmark();
	}
}
