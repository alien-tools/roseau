package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.nio.file.Path;

public final class GenerateCombinatorialApi extends AbstractStep {
	private API api = null;

	public GenerateCombinatorialApi(Path outputPath) {
		super(outputPath);
	}

	public void run() {
		try {
			var apiWriter = new ApiWriter(outputPath);
			apiWriter.createOutputHierarchy();

			var combinatorialApi = new CombinatorialApi();
			combinatorialApi.build();

			api = combinatorialApi.getAPI();
			System.out.println("api=" + api);
			apiWriter.write(api);

			ApiStats.display(api);
		} catch (Exception e) {
			System.err.println("Failed to build API in " + outputPath);
			System.err.println(e.getMessage());

			System.exit(1);
		}
	}

	public API getGeneratedApi() {
		return api;
	}
}
