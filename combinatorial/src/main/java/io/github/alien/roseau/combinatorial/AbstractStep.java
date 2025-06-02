package io.github.alien.roseau.combinatorial;

import java.nio.file.Path;

public abstract class AbstractStep {
	protected final Path outputPath;

	public AbstractStep(Path outputPath) {
		this.outputPath = outputPath;
	}

	public abstract void run() throws StepExecutionException;
}
