package com.github.maracas.roseau.combinatorial;

import java.nio.file.Path;

public abstract class AbstractStep {
	protected final Path outputPath;

	public AbstractStep(Path outputPath) {
		this.outputPath = outputPath;
	}

	public abstract void run();

	protected static void checkPath(Path path) {
		if (!path.toFile().exists()) {
			System.err.println("Directory " + path + " does not exist");
			System.exit(1);
		}
	}
}
