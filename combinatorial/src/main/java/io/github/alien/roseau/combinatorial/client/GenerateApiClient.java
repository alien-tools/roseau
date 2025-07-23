package io.github.alien.roseau.combinatorial.client;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.writer.ClientWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class GenerateApiClient {
	private static final Logger LOGGER = LogManager.getLogger(GenerateApiClient.class);

	public GenerateApiClient() {}

	public void run(API api, Path outputPath) throws StepExecutionException {
		var clientWriter = new ClientWriter(outputPath);

		try {
			LOGGER.info("-- Generating client for API --");
			LOGGER.info(outputPath.toFile().getName());
			new ClientGeneratorVisitor(clientWriter).$(api).visit();

			clientWriter.writeClientFile();
			LOGGER.info("-- Client generated for API ---");
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}
}
