package io.github.alien.roseau.combinatorial.mode;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.combinatorial.AbstractStep;
import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.client.GenerateApiClient;
import io.github.alien.roseau.combinatorial.compiler.InternalJavaCompiler;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public final class GenerateClient extends AbstractStep {
	private static final Logger LOGGER = LogManager.getLogger(GenerateClient.class);

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	private final Path apiPath;
	private final Path tmpOutputPath;

	public GenerateClient(Path apiPath, Path outputPath, Path tmpOutputPath) {
		super(outputPath);

		this.apiPath = apiPath;
		this.tmpOutputPath = tmpOutputPath;
	}

	@Override
	public void run() throws StepExecutionException {
		var currentNow = System.currentTimeMillis();

		LOGGER.info("Starting combinatorial client generation...");

		var types = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()))
			.extractTypes(Library.of(apiPath));
		if (types == null) {
			throw new StepExecutionException(this.getClass().getSimpleName(), "Failed to extract API from %s".formatted(apiPath));
		}

		var apiName = apiPath.toFile().getName();
		var clientSourcePath = outputPath.resolve(apiName);

		new GenerateApiClient(types.toAPI(), clientSourcePath).run();

		try {
			ExplorerUtils.cleanOrCreateDirectory(tmpOutputPath);

			var clientBinPath = tmpOutputPath.resolve(Path.of(Constants.BINARIES_FOLDER, apiName));
			var apiJarPath = tmpOutputPath.resolve(Path.of(Constants.JAR_FOLDER, "%s.jar".formatted(apiName)));

			compiler.checkClientCompilesWithApi(clientSourcePath, apiPath, clientBinPath, apiJarPath);
		} catch (Exception e) {
			throw new StepExecutionException(this.getClass().getSimpleName(), e.getMessage());
		}

		LOGGER.info("Combinatorial clients generation took {} ms", System.currentTimeMillis() - currentNow);
	}
}
