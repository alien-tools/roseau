package io.github.alien.roseau.combinatorial.client;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.writer.ClientWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class GenerateApiClient extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger(GenerateApiClient.class);

	private final API api;

	private final ClientWriter clientWriter;

	public GenerateApiClient(API api, Path clientOutputPath) {
		super(clientOutputPath);

		this.api = api;

		clientWriter = new ClientWriter(clientOutputPath, api);
	}

	public void run() throws StepExecutionException {
		if (!clientWriter.createOutputHierarchy())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Failed to create client sources hierarchy");

		try {
			LOGGER.info("-- Generating client for API --");
			new ClientGeneratorVisitor(api, clientWriter).$(api).visit();

			clientWriter.writeClientFile();
			LOGGER.info("-- Client generated for API ---\n");
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}
}
