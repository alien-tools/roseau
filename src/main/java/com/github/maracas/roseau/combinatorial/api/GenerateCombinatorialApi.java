package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.nio.file.Path;

public final class GenerateCombinatorialApi extends AbstractStep {
	public GenerateCombinatorialApi(Path outputPath) {
		super(outputPath);
	}

	public void run() throws StepExecutionException {
		var apiWriter = new ApiWriter(outputPath);
		if (!apiWriter.createOutputHierarchy())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Failed to create output hierarchy");

		try {
			var combinatorialApi = new CombinatorialApi();
			combinatorialApi.build();

			var api = combinatorialApi.getAPI();
			apiWriter.write(api);

			ApiStats.display(api);
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}
}
