package io.github.alien.roseau.combinatorial.api;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.writer.ApiWriter;

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
			api = combinatorialApi.build();

			apiWriter.write(api);

			ApiStats.display(api);
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}

	public API getApi() throws StepExecutionException {
		if (api == null) throw new StepExecutionException(this.getClass().getSimpleName(), "API is null");

		return api;
	}
}
