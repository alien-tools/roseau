package io.github.alien.roseau.combinatorial;

import io.github.alien.roseau.combinatorial.client.GenerateApiClient;
import io.github.alien.roseau.combinatorial.compiler.CompileClientAndV1;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;

public final class GenerateClients {
	private static final Logger LOGGER = LogManager.getLogger(GenerateClients.class);

	public static void main(String[] args) {
		var clientsOutputPath = Path.of(Constants.OUTPUT_FOLDER, Constants.CLIENTS_FOLDER);
		var v1sPath = Path.of(Constants.V1s_FOLDER);

		try {
			var currentNow = System.currentTimeMillis();

			LOGGER.info("Starting combinatorial clients generation...");
			var clientGeneration = new GenerateApiClient();
			var compiler = new CompileClientAndV1();

			Arrays.stream(v1sPath.toFile().listFiles()).toList().forEach(v1Folder -> {
				if (!v1Folder.isDirectory()) return;
				var v1Path = v1Folder.toPath();

				var types = new JdtTypesExtractor().extractTypes(v1Path);
				if (types == null) {
					LOGGER.error("Failed to extract API from {}", v1Folder.getName());
					return;
				}
				var api = types.toAPI();

				Path clientPath = clientsOutputPath.resolve(v1Folder.getName());
				try {
					clientGeneration.run(api, clientPath);
				} catch (Exception e) {
					LOGGER.error("Failed to generate client for API in {}", v1Folder.getName());
					LOGGER.error(e.getMessage());
					return;
				}

				try {
					compiler.checkClientCompilesWithV1(clientPath, v1Path);
				} catch (Exception e) {
					LOGGER.error("Failed to compile client for API in {}", v1Folder.getName());
					LOGGER.error(e.getMessage());
				}
			});

			LOGGER.info("\nCombinatorial clients generation took {} ms", System.currentTimeMillis() - currentNow);
		} catch (Exception e) {
			LOGGER.error("Failed to run combinatorial clients generation");
			LOGGER.error(e.getMessage());

			System.exit(1);
		}
	}
}
