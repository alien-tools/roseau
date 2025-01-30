package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.StepExecutionException;
import com.github.maracas.roseau.combinatorial.writer.ClientWriter;

import java.nio.file.Path;

public final class GenerateApiClients extends AbstractStep {
	private final API api;

	public GenerateApiClients(API api, Path outputPath) {
		super(outputPath);

		this.api = api;
	}

	public void run() throws StepExecutionException {
		var clientWriter = new ClientWriter(outputPath);

		try {
			System.out.println("\n-- Generating clients for API --");
			new ClientGeneratorVisitor(clientWriter).$(api).visit();
			System.out.println("-- Clients generated for API ---\n");
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}
	}
}
