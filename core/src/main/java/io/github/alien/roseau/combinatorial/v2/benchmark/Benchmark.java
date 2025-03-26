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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Benchmark implements Runnable {
	private static final Logger LOGGER = LogManager.getLogger(Benchmark.class);

	private final String id;

	private final Path benchmarkWorkingPath;
	private final Path clientBinPath;
	private final Path clientSourcePath;
	private final Path v2SourcesPath;
	private final Path v2JarPath;

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
			Path clientBinPath,
			Path clientSourcePath,
			Path v1JarPath,
			Path tmpPath
	) {
		LOGGER.info("Creating Benchmark {}", id);
		this.id = id;

		this.benchmarkWorkingPath = tmpPath.resolve(id);
		this.clientBinPath = clientBinPath;
		this.clientSourcePath = clientSourcePath;
		this.v2SourcesPath = benchmarkWorkingPath.resolve(Constants.API_FOLDER);
		this.v2JarPath = benchmarkWorkingPath.resolve(Path.of(Constants.JAR_FOLDER, "v2.jar"));

		this.apiQueue = apiQueue;
		this.resultsQueue = resultsQueue;

		this.tools = List.of(
				new JapicmpTool(v1JarPath, v2JarPath),
				new RevapiTool(v1JarPath, v2JarPath),
				new RoseauTool(v1JarPath, v2JarPath)
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
				var v2Api = strategyAndApi.getValue1();

				LOGGER.info("--------------------------------");
				LOGGER.info("Running Benchmark Thread n°{}", id);
				LOGGER.info("Breaking Change: {}", strategy);

				generateNewApiSourcesAndJar(v2Api);
				var groundTruth = generateGroundTruth();
				runToolsAnalysis(strategy, groundTruth);

				LOGGER.info("Benchmark Thread n°{} finished", id);
				LOGGER.info("--------------------------------\n");
			} catch (Exception e) {
				errorsCount++;
				LOGGER.info("Benchmark Thread n°{} failed: {}", id, e.getMessage());
			}
		}

		if (errorsCount == 0)
			ExplorerUtils.removeDirectory(benchmarkWorkingPath);
	}

	public void informApisGenerationIsOver() {
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
		LOGGER.info("Generated to {}", v2SourcesPath);

		LOGGER.info("Generating new API Jar");
		var errors = compiler.packageApiToJar(v2SourcesPath, v2JarPath);
		if (!errors.isEmpty())
			throw new RuntimeException("Failed to package new api to jar");
		LOGGER.info("Generated to {}", v2JarPath);
	}

	private ToolResult generateGroundTruth() {
		LOGGER.info("Generating Ground Truth");

		long startTime = System.currentTimeMillis();

		var tmpClientsBinPath = benchmarkWorkingPath.resolve(Constants.BINARIES_FOLDER);
		var sourceErrors = compiler.compileClientWithApi(clientSourcePath, v2JarPath, tmpClientsBinPath);
		var isSourceBreaking = !sourceErrors.isEmpty();

		var binaryErrors = compiler.linkClientWithApi(clientBinPath, v2JarPath, Constants.CLIENT_FILENAME, Constants.CLIENT_FOLDER);
		var isBinaryBreaking = !binaryErrors.isEmpty();

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Ground Truth", executionTime, isBinaryBreaking, isSourceBreaking);
	}

	private void runToolsAnalysis(String strategy, ToolResult groundTruth) {
		LOGGER.info("--------------------------------");
		LOGGER.info("     Running Tools Analysis");

		var results = new ArrayList<>(Collections.singletonList(groundTruth));
		for (var tool : tools) {
			LOGGER.info("--------------------------------");
			LOGGER.info(" Running {}", tool.getClass().getSimpleName());

			var result = tool.detectBreakingChanges();
			if (result == null) {
				LOGGER.info(" Tool Result: N/A");
				continue;
			}

			results.add(result);

			LOGGER.info(" Execution Time    : {}ms", result.executionTime());
			LOGGER.info(" Source Tool Result: {}", result.isSourceBreaking() ? "Breaking" : "Not Breaking");
			LOGGER.info(" Source Expected   : {}", groundTruth.isSourceBreaking() ? "Breaking" : "Not Breaking");
			LOGGER.info(" Source Result     : {}", result.isSourceBreaking() == groundTruth.isSourceBreaking() ? "OK" : "KO");
			LOGGER.info(" Binary Tool Result: {}", result.isBinaryBreaking() ? "Breaking" : "Not Breaking");
			LOGGER.info(" Binary Expected   : {}", groundTruth.isBinaryBreaking() ? "Breaking" : "Not Breaking");
			LOGGER.info(" Binary Result     : {}", result.isBinaryBreaking() == groundTruth.isBinaryBreaking() ? "OK" : "KO");
		}

		LOGGER.info("--------------------------------");

		resultsQueue.put(strategy, results);
	}
}
