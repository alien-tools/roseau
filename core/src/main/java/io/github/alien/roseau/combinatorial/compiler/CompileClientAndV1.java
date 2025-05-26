package io.github.alien.roseau.combinatorial.compiler;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.StepExecutionException;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class CompileClientAndV1 {
	private static final Logger LOGGER = LogManager.getLogger(CompileClientAndV1.class);

	private final InternalJavaCompiler compiler = new InternalJavaCompiler();

	private final Path v1sJarPath;
	private final Path clientsBinPath;

	public CompileClientAndV1() {
		Path tmpPath = Path.of(Constants.TMP_FOLDER);

		v1sJarPath = tmpPath.resolve(Path.of(Constants.JAR_FOLDER));
		clientsBinPath = tmpPath.resolve(Constants.BINARIES_FOLDER);

		ExplorerUtils.cleanOrCreateDirectory(tmpPath);
	}

	public void checkClientCompilesWithV1(Path clientSourcesPath, Path v1SourcesPath) throws StepExecutionException {
		checkSourcesArePresent(clientSourcesPath, v1SourcesPath);

		var v1Name = v1SourcesPath.toFile().getName();
		var v1JarPath = v1sJarPath.resolve("%s.jar".formatted(v1Name));
		var clientBinPath = clientsBinPath.resolve(v1Name);

		packageV1Api(v1SourcesPath, v1JarPath);
		compileClient(clientSourcesPath, clientBinPath, v1JarPath);
	}

	private void checkSourcesArePresent(Path clientSourcesPath, Path v1SourcesPath) throws StepExecutionException {
		if (!ExplorerUtils.checkPathExists(v1SourcesPath))
			throw new StepExecutionException(this.getClass().getSimpleName(), "V1 API sources are missing");

		if (!ExplorerUtils.checkPathExists(clientSourcesPath))
			throw new StepExecutionException(this.getClass().getSimpleName(), "Clients sources are missing");
	}

	private void packageV1Api(Path v1SourcesPath, Path v1JarPath) throws StepExecutionException {
		var errors = compiler.packageApiToJar(v1SourcesPath, v1JarPath);

		if (!errors.isEmpty())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Couldn't package V1 API: " + formatCompilerErrors(errors));
	}

	private void compileClient(Path clientSourcePath, Path clientBinPath, Path v1JarPath) throws StepExecutionException {
		var errors = compiler.compileClientWithApi(clientSourcePath, Constants.CLIENT_FILENAME, v1JarPath, clientBinPath);

		if (!errors.isEmpty())
			throw new StepExecutionException(this.getClass().getSimpleName(), "Couldn't compile client: " + formatCompilerErrors(errors));

		LOGGER.info("-------- Client compiled -------");
	}

	private static String formatCompilerErrors(List<?> errors) {
		return errors.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
	}
}
