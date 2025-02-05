package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.io.IOException;
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
			exportApiToJson();

			ApiStats.display(api);
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}

	public API getApi() throws StepExecutionException {
		if (api == null) throw new StepExecutionException(this.getClass().getSimpleName(), "API is null");

		return api;
	}

	private void exportApiToJson() throws StepExecutionException {
		if (api == null) throw new StepExecutionException(this.getClass().getSimpleName(), "API is null");

		try {
			var exportPath = outputPath.resolve(Constants.API_JSON);
			api.writeJson(exportPath);
		} catch (IOException e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), "Failed to export generated API:" + e.getMessage());
		}
	}
}
