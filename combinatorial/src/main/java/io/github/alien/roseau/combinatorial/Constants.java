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

	public static final Path ERRORS_PATH = Path.of(OUTPUT_FOLDER, "errors");
	public static final Path IMPOSSIBLE_STRATEGIES_PATH = Path.of(OUTPUT_FOLDER, "impossible_strategies");
	public static final Path RESULTS_PATH = Path.of(OUTPUT_FOLDER, "results");
}
