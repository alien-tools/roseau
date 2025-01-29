package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.writer.ClientWriter;

import java.nio.file.Path;

public class ClientsGenerator {
	public static void main(String[] args) {
		String apiDir = args.length >= 1 ? args[0] : Constants.DEFAULT_API_DIR;
		String outputDir = args.length >= 2 ? args[1] : Constants.DEFAULT_OUTPUT_DIR;

		Path apiPath = Path.of(apiDir);
		if (!apiPath.toFile().exists()) {
			System.err.println("API directory " + apiDir + " does not exist");
			System.exit(1);
		}

		try {
			APIExtractor apiExtractor = new SpoonAPIExtractor();
			var api = apiExtractor.extractAPI(apiPath);

			var clientWriter = new ClientWriter(Path.of(outputDir));

			new ClientGeneratorVisitor(clientWriter).$(api).visit();
		} catch (Exception e) {
			System.err.println("Failed to extract API from " + apiDir);
			System.err.println(e.getMessage());

			System.exit(1);
		}
	}
}
