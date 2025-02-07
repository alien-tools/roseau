package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.writer.ClientWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class GenerateApiClients extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger();

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
