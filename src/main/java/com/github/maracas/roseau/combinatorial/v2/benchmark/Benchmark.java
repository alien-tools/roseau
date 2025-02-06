package com.github.maracas.roseau.combinatorial.v2.benchmark;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.utils.ExplorerUtils;
import com.github.maracas.roseau.combinatorial.v2.queue.NewApiQueue;
import com.github.maracas.roseau.combinatorial.v2.queue.ResultsProcessQueue;
import com.github.maracas.roseau.combinatorial.v2.benchmark.result.ToolResult;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.AbstractTool;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.JapicmpTool;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.RevapiTool;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.RoseauTool;
import com.github.maracas.roseau.combinatorial.v2.compiler.InternalJavaCompiler;
import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Benchmark implements Runnable {
	private final String id;

	private final Path benchmarkWorkingPath;
	private final Path clientsSourcesPath;
	private final Path v2SourcesPath;
	private final Path v2JarPath;

	private final NewApiQueue apiQueue;
	private final ResultsProcessQueue resultsQueue;

	private final List<AbstractTool> tools;

	private final ApiWriter apiWriter;

	private boolean isNewBreakingApisGenerationOngoing = true;
	private int errorsCount = 0;

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	public Benchmark(
			String id,
			NewApiQueue apiQueue,
			ResultsProcessQueue resultsQueue,
			Path clientsSourcesPath,
			Path v1SourcesPath,
			Path v1JarPath,
			Path tmpPath
	) {
		System.out.println("Creating Benchmark " + id);
		this.id = id;

		this.benchmarkWorkingPath = tmpPath.resolve(id);
		this.clientsSourcesPath = clientsSourcesPath;
		this.v2SourcesPath = benchmarkWorkingPath.resolve(Constants.API_FOLDER);
		this.v2JarPath = benchmarkWorkingPath.resolve(Path.of(Constants.JAR_FOLDER, "v2.jar"));

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
		while (isNewBreakingApisGenerationOngoing || apiQueue.hasStillWork()) {
			var strategyAndApi = apiQueue.take();
			if (strategyAndApi == null) break;

			try {
				var strategy = strategyAndApi.getValue0();
				var api = strategyAndApi.getValue1();

				System.out.println("\n--------------------------------");
				System.out.println("Running Benchmark Thread n°" + id);
				System.out.println("Breaking Change: " + strategy);

				generateNewApiSourcesAndJar(api);
				var newApiIsBreaking = generateGroundTruth();
				runToolsAnalysis(strategy, newApiIsBreaking);

				System.out.println("Benchmark Thread n°" + id + " finished");
				System.out.println("--------------------------------\n");
			} catch (Exception e) {
				errorsCount++;
				System.out.println("Benchmark Thread n°" + id + " failed: " + e.getMessage());
			}
		}

		if (errorsCount == 0)
			ExplorerUtils.removeDirectory(benchmarkWorkingPath);
	}

	public void informsBreakingApisGenerationIsOver() {
		isNewBreakingApisGenerationOngoing = false;
	}

	public int getErrorsCount() {
		return errorsCount;
	}

	private void generateNewApiSourcesAndJar(API api) {
		ExplorerUtils.cleanOrCreateDirectory(benchmarkWorkingPath);

		System.out.println("\nGenerating new API Sources");
		if (!apiWriter.createOutputHierarchy())
			throw new RuntimeException("Failed to create new api sources hierarchy");
		apiWriter.write(api);
		System.out.println("Generated to " + v2SourcesPath);

		System.out.println("\nGenerating new API Jar");
		var errors = compiler.packageApiToJar(v2SourcesPath, v2JarPath);
		if (!errors.isEmpty())
			throw new RuntimeException("Failed to package new api to jar");
		System.out.println("Generated to " + v2JarPath);
	}

	private boolean generateGroundTruth() {
		System.out.println("\nGenerating Ground Truth");

		var tmpClientsBinPath = benchmarkWorkingPath.resolve(Constants.BINARIES_FOLDER);
		var errors = compiler.compileClientWithApi(clientsSourcesPath, v2JarPath, tmpClientsBinPath);
		return !errors.isEmpty();
	}

	private void runToolsAnalysis(String strategy, boolean isBreaking) {
		System.out.println("\n--------------------------------");
		System.out.println("     Running Tools Analysis");

		var results = new ArrayList<ToolResult>();
		for (var tool : tools) {
			System.out.println("--------------------------------");
			System.out.println(" Running " + tool.getClass().getSimpleName());

			var result = tool.detectBreakingChanges();
			results.add(result);

			System.out.println(" Execution Time: " + result.executionTime() + "ms");
			System.out.println(" Tool Result   : " + (result.isBreaking() ? "Breaking" : "Not Breaking"));
			System.out.println(" Expected      : " + (isBreaking ? "Breaking" : "Not Breaking"));
			System.out.println(" Result        : " + (result.isBreaking() == isBreaking ? "OK" : "KO"));
		}

		System.out.println("--------------------------------\n");

		resultsQueue.put(strategy, results);
	}
}
