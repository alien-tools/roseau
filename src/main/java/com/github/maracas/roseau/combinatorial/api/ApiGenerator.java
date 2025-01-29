package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.nio.file.Path;

public class ApiGenerator {
	public static void main(String[] args) {
		String outputDir = args.length >= 1 ? args[0] : "output";

		try {
			var apiWriter = new ApiWriter(Path.of(outputDir));
			apiWriter.createOutputDir();

			var combinatorialApi = new CombinatorialApi();
			combinatorialApi.build();

			var api = combinatorialApi.getAPI();
			System.out.println("api="+api);
			apiWriter.write(api);

			ApiStats.display(api);
		} catch (Exception e) {
			System.err.println("Failed to build API in " + outputDir);
			System.err.println(e.getMessage());

			System.exit(1);
		}
	}
}
