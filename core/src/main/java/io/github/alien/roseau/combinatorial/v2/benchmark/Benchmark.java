package io.github.alien.roseau.combinatorial.v2.benchmark;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import io.github.alien.roseau.combinatorial.v2.benchmark.tool.AbstractTool;
import io.github.alien.roseau.combinatorial.v2.benchmark.tool.JapicmpTool;
import io.github.alien.roseau.combinatorial.v2.benchmark.tool.RevapiTool;
import io.github.alien.roseau.combinatorial.v2.benchmark.tool.RoseauTool;
import io.github.alien.roseau.combinatorial.v2.compiler.InternalJavaCompiler;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;
import io.github.alien.roseau.combinatorial.v2.queue.ResultsProcessQueue;
import io.github.alien.roseau.combinatorial.writer.ApiWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Benchmark implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(Benchmark.class);

	private final String id;

	private final Path benchmarkWorkingPath;
	private final Path clientsSourcesPath;
	private final Path v2SourcesPath;
	private final Path v2JarPath;

	private final API v1Api;

	private final NewApiQueue apiQueue;
	private final ResultsProcessQueue resultsQueue;

	private final List<AbstractTool> tools;

	private final ApiWriter apiWriter;

	private boolean isNewApisGenerationOngoing = true;
	private int errorsCount = 0;

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	public Benchmark(
			String id,
			NewApiQueue apiQueue,
			ResultsProcessQueue resultsQueue,
			Path clientsSourcesPath,
			Path v1SourcesPath,
			Path v1JarPath,
			Path tmpPath,
			API v1Api
	) {
		LOGGER.info("Creating Benchmark " + id);
		this.id = id;

		this.benchmarkWorkingPath = tmpPath.resolve(id);
		this.clientsSourcesPath = clientsSourcesPath;
		this.v2SourcesPath = benchmarkWorkingPath.resolve(Constants.API_FOLDER);
		this.v2JarPath = benchmarkWorkingPath.resolve(Path.of(Constants.JAR_FOLDER, "v2.jar"));

		this.v1Api = v1Api;

		this.apiQueue = apiQueue;
		this.resultsQueue = resultsQueue;

		this.tools = List.of(
				new JapicmpTool(v1JarPath, v2JarPath),
				new RevapiTool(v1JarPath, v2JarPath),
				new RoseauTool(v1SourcesPath, v2SourcesPath)
		);

		this.apiWriter = new ApiWriter(benchmarkWorkingPath);
	}

	@Override
	public void run() {
		while (isNewApisGenerationOngoing || apiQueue.hasStillWork()) {
			var strategyAndApi = apiQueue.poll();
			if (strategyAndApi == null) break;

			try {
				var strategy = strategyAndApi.getValue0();
				var symbol = strategyAndApi.getValue1().getValue0();
				var v2Api = strategyAndApi.getValue1().getValue1();

				LOGGER.info("--------------------------------");
				LOGGER.info("Running Benchmark Thread n°" + id);
				LOGGER.info("Breaking Change: " + strategy);

				generateNewApiSourcesAndJar(v2Api);
				var newApiIsBreaking = generateGroundTruth();
				runToolsAnalysis(strategy, v2Api, newApiIsBreaking);

				LOGGER.info("Benchmark Thread n°" + id + " finished");
				LOGGER.info("--------------------------------\n");
			} catch (Exception e) {
				errorsCount++;
				LOGGER.info("Benchmark Thread n°" + id + " failed: " + e.getMessage());
			}
		}

		if (errorsCount == 0)
			ExplorerUtils.removeDirectory(benchmarkWorkingPath);
	}

	public void informsApisGenerationIsOver() {
		isNewApisGenerationOngoing = false;
	}

	public int getErrorsCount() {
		return errorsCount;
	}

	private void generateNewApiSourcesAndJar(API api) {
		ExplorerUtils.cleanOrCreateDirectory(benchmarkWorkingPath);

		LOGGER.info("Generating new API Sources");
		if (!apiWriter.createOutputHierarchy())
			throw new RuntimeException("Failed to create new api sources hierarchy");
		apiWriter.write(api);
		LOGGER.info("Generated to " + v2SourcesPath);

		LOGGER.info("Generating new API Jar");
		var errors = compiler.packageApiToJar(v2SourcesPath, v2JarPath);
		if (!errors.isEmpty())
			throw new RuntimeException("Failed to package new api to jar");
		LOGGER.info("Generated to " + v2JarPath);
	}

	private boolean generateGroundTruth() {
		LOGGER.info("Generating Ground Truth");

		var tmpClientsBinPath = benchmarkWorkingPath.resolve(Constants.BINARIES_FOLDER);
		var errors = compiler.compileClientWithApi(clientsSourcesPath, v2JarPath, tmpClientsBinPath);
		return !errors.isEmpty();
	}

	private void runToolsAnalysis(String strategy, API v2Api, boolean isBreaking) {
		LOGGER.info("--------------------------------");
		LOGGER.info("     Running Tools Analysis");

		var results = new ArrayList<ToolResult>();
		for (var tool : tools) {
			LOGGER.info("--------------------------------");
			LOGGER.info(" Running " + tool.getClass().getSimpleName());

			if (tool instanceof RoseauTool roseauTool) {
				roseauTool.setApis(v1Api, v2Api);
			}

			var result = tool.detectBreakingChanges();
			if (result == null) {
				LOGGER.info(" Tool Result: N/A");
				continue;
			}

			results.add(result);

			LOGGER.info(" Execution Time: " + result.executionTime() + "ms");
			LOGGER.info(" Tool Result   : " + (result.isBreaking() ? "Breaking" : "Not Breaking"));
			LOGGER.info(" Expected      : " + (isBreaking ? "Breaking" : "Not Breaking"));
			LOGGER.info(" Result        : " + (result.isBreaking() == isBreaking ? "OK" : "KO"));
		}

		LOGGER.info("--------------------------------");

		resultsQueue.put(strategy, new Pair<>(isBreaking, results));
	}
}
