package io.github.alien.roseau.combinatorial.client;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.writer.ClientWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class GenerateApiClients extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger(GenerateApiClients.class);

	private final API api;

	public GenerateApiClients(API api, Path outputPath) {
		super(outputPath);

		this.api = api;
	}

	public void run() throws StepExecutionException {
		var clientWriter = new ClientWriter(outputPath);

		try {
			LOGGER.info("-- Generating clients for API --");
			new ClientGeneratorVisitor(clientWriter).$(api).visit();
			LOGGER.info("-- Clients generated for API ---\n");
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}
}
