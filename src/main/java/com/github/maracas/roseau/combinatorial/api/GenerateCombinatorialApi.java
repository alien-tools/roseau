package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.nio.file.Path;

public final class GenerateCombinatorialApi extends AbstractStep {
	private API api = null;

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

			api = combinatorialApi.getAPI();
			apiWriter.write(api);

			ApiStats.display(api);
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}

	public API getGeneratedApi() throws StepExecutionException {
		if (api == null) throw new StepExecutionException(this.getClass().getSimpleName(), "API not generated yet");

		return api;
	}
}
