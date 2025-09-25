package io.github.alien.roseau.combinatorial.v2.filter;

import io.github.alien.roseau.combinatorial.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Set;

public final class PreviousFailuresFilter extends StrategyFilter {
	private static PreviousFailuresFilter _instance = null;

	private final Path failedStrategiesPath;
	private final Path impossibleStrategiesPath;

	private PreviousFailuresFilter(boolean isEnabled, Path outputPath) {
		super(isEnabled);

		failedStrategiesPath = Constants.getFailedStrategiesPath(outputPath);
		impossibleStrategiesPath = Constants.getImpossibleStrategiesPath(outputPath);

		fillStrategiesFilter();
	}

	public static void initialize(boolean isEnabled, Path outputPath) {
		if (_instance == null) {
			_instance = new PreviousFailuresFilter(isEnabled, outputPath);
		}
	}

	public static PreviousFailuresFilter getInstance() {
		if (_instance == null) {
			throw new IllegalStateException("PreviousFailuresFilter is not initialized. Call getInstance(boolean isEnabled) first.");
		}
		return _instance;
	}

	@Override
	protected void fillStrategiesFilter() {
		if (!isEnabled) return;

		var lastFailedFile = getLastFileInPath(failedStrategiesPath);
		if (lastFailedFile != null) {
			var failedStrategies = getStrategiesFromFile(lastFailedFile);
			strategiesFilter.addAll(failedStrategies);
		}

		var lastImpossibleFile = getLastFileInPath(impossibleStrategiesPath);
		if (lastImpossibleFile != null) {
			var impossibleStrategies = getStrategiesFromFile(lastImpossibleFile);
			strategiesFilter.addAll(impossibleStrategies);
		}
	}

	private static File getLastFileInPath(Path path) {
		var files = path.toFile().listFiles();
		if (files == null || files.length == 0) {
			return null;
		}

		File lastFile = null;
		for (File file : files) {
			if (lastFile == null || file.lastModified() > lastFile.lastModified()) {
				lastFile = file;
			}
		}
		return lastFile;
	}

	private static Set<String> getStrategiesFromFile(File file) {
		if (file == null || !file.exists() || !file.isFile()) return Set.of();

		try (var reader = new BufferedReader(new FileReader(file))) {
			return reader.lines()
					.filter(line -> !line.isBlank())
					.map(line -> line.split(",")[0].trim())
					.collect(java.util.stream.Collectors.toSet());
		} catch (Exception ignored) {
			return Set.of();
		}
	}
}
