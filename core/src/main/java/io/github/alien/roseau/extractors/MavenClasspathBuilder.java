package io.github.alien.roseau.extractors;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Utility class to automatically infer the classpath of a Maven software library. This implementation attempts to
 * retrieve the classpath from a supplied {@code pom.xml} file using {@code mvn dependency:build-classpath}.
 */
public class MavenClasspathBuilder {
	private static final Logger LOGGER = LogManager.getLogger(MavenClasspathBuilder.class);

	/**
	 * Returns the classpath of the supplied {@code pom.xml} file or pom-containing directory using
	 * {@code mvn dependency:build-classpath}.
	 *
	 * @param pom the {@code pom.xml} file or pom-containing directory to analyze
	 * @return the retrieved classpath or an empty list if something went wrong
	 */
	public List<Path> buildClasspath(Path pom) {
		Preconditions.checkArgument(pom != null && Files.isRegularFile(pom));
		Path classpathFile = pom.toFile().isDirectory()
			? pom.toAbsolutePath().resolve(".classpath.tmp")
			: pom.toAbsolutePath().getParent().resolve(".classpath.tmp");

		try {
			InvocationRequest request = makeClasspathRequest(pom, classpathFile);
			Invoker invoker = new DefaultInvoker();
			InvocationResult result = invoker.execute(request);

			if (result.getExitCode() == 0 && Files.isRegularFile(classpathFile)) {
				String cpString = Files.readString(classpathFile);
				return Arrays.stream(cpString.split(File.pathSeparator))
					.map(Path::of)
					.toList();
			} else {
				LOGGER.warn("Failed to build Maven classpath from {}", pom, result.getExecutionException());
			}
		} catch (Exception e) {
			// We may encounter RuntimeExceptions
			LOGGER.warn("Failed to build Maven classpath from {}", pom, e);
		} finally {
			try {
				Files.deleteIfExists(classpathFile);
			} catch (IOException e) {
				LOGGER.warn("Couldn't delete temporary classpath file {}", classpathFile, e);
			}
		}

		return List.of();
	}

	private static InvocationRequest makeClasspathRequest(Path pom, Path classpathFile) {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(pom.toAbsolutePath().toFile());
		request.setBatchMode(true);
		request.addArg("dependency:build-classpath");
		request.setReactorFailureBehavior(InvocationRequest.ReactorFailureBehavior.FailNever);
		Properties properties = new Properties();
		properties.setProperty("mdep.outputFile", classpathFile.toAbsolutePath().toString());
		// "An empty string indicates include all dependencies"
		properties.setProperty("mdep.includeScope", "");
		request.setProperties(properties);
		request.setOutputHandler(LOGGER::debug);
		request.setErrorHandler(LOGGER::warn);
		return request;
	}
}
