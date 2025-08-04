package io.github.alien.roseau.combinatorial.mode;

import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.api.GenerateCombinatorialApi;
import io.github.alien.roseau.combinatorial.client.GenerateApiClient;
import io.github.alien.roseau.combinatorial.v2.GenerateNewVersionsAndLaunchBenchmark;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class CombinatorialBenchmark extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger(CombinatorialBenchmark.class);

	private final int maxParallelAnalysis;
	private final Path tmpOutputPath;

	public CombinatorialBenchmark(int threads, Path outputPath, Path tmpOutputPath) {
		super(outputPath);

		this.maxParallelAnalysis = threads - 2;
		this.tmpOutputPath = tmpOutputPath;
	}

	@Override
	public void run() throws StepExecutionException {
		var currentNow = System.currentTimeMillis();

		var apiGeneration = new GenerateCombinatorialApi(outputPath);
		apiGeneration.run();
		var api = apiGeneration.getApi();

		var clientGeneration = new GenerateApiClient(api, outputPath.resolve(Constants.CLIENT_FOLDER));
		clientGeneration.run();

		var newVersionsAndBenchmarkStep = new GenerateNewVersionsAndLaunchBenchmark(api, maxParallelAnalysis, outputPath, tmpOutputPath);
		newVersionsAndBenchmarkStep.run();

		LOGGER.info("\nCombinatorial benchmark took {} ms", System.currentTimeMillis() - currentNow);
	}
}
