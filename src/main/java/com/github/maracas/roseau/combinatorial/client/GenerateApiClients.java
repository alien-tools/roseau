package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.AbstractStep;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.writer.ClientWriter;

import java.nio.file.Path;

public final class GenerateApiClients extends AbstractStep {
	private final API api;

	public GenerateApiClients(API api, Path outputPath) {
		super(outputPath);

		this.api = api;
	}

	public void run() {
		checkPath(outputPath.resolve(Constants.API_FOLDER));

		var clientWriter = new ClientWriter(outputPath);

		System.out.println("\n--------------------------------");
		System.out.println("-- Generating clients for API --");
		new ClientGeneratorVisitor(clientWriter).$(api).visit();
		System.out.println("-- Generated clients for API ---");
		System.out.println("--------------------------------\n");
	}
}
