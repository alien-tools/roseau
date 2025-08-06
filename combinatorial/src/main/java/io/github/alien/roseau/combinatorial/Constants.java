package io.github.alien.roseau.combinatorial;

import java.nio.file.Path;

public final class Constants {
	public static final String API_FOLDER = "api";
	public static final String BINARIES_FOLDER = "bin";
	public static final String CLIENT_FOLDER = "client";
	public static final String CLIENT_FILENAME = "FullClient";
	public static final String JAR_FOLDER = "jar";
	public static final String OUTPUT_FOLDER = "output";
	public static final String TMP_FOLDER = "tmp";
	public static final String ERRORS_FOLDER = "errors";
	public static final String IMPOSSIBLE_STRATEGIES_FOLDER = "impossible_strategies";
	public static final String RESULTS_FOLDER = "results";

	public static Path getFailedStrategiesPath(Path outputPath) {
		return outputPath.resolve(ERRORS_FOLDER);
	}

	public static Path getImpossibleStrategiesPath(Path outputPath) {
		return outputPath.resolve(IMPOSSIBLE_STRATEGIES_FOLDER);
	}

	public static Path getResultsPath(Path outputPath) {
		return outputPath.resolve(RESULTS_FOLDER);
	}
}
