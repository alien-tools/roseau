package io.github.alien.roseau;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to automatically infer the classpath of a Maven library. This implementation attempts to retrieve the
 * classpath from a supplied {@code pom.xml} file using {@code mvn dependency:build-classpath}.
 */
public class MavenClasspathBuilder {
	private static final Logger LOGGER = LogManager.getLogger(MavenClasspathBuilder.class);

	/**
	 * Returns the classpath of the supplied {@code pom.xml} file using {@code mvn dependency:build-classpath}.
	 *
	 * @param pom the {@code pom.xml} file
	 * @return the retrieved classpath or an empty list if something went wrong
	 */
	public Set<Path> buildClasspath(Path pom) {
		Preconditions.checkNotNull(pom);

		if (!Files.isRegularFile(pom)) {
			LOGGER.warn("Invalid pom.xml file {}", pom);
		}

		String random = Long.toHexString(Double.doubleToLongBits(Math.random()));
		Path classpathFile = pom.resolveSibling(".roseau-classpath-" + random + ".tmp");
		try {
			Optional<File> mvnExecutable = findMavenExecutable(pom);

			if (mvnExecutable.isPresent()) {
				InvocationRequest request = makeClasspathRequest(pom, classpathFile);
				Invoker invoker = new DefaultInvoker();
				invoker.setMavenExecutable(mvnExecutable.get());
				InvocationResult result = invoker.execute(request);

				if (result.getExitCode() == 0 && Files.isRegularFile(classpathFile)) {
					String cpString = Files.readString(classpathFile);
					LOGGER.debug("Extracted classpath from {}", pom);
					return Arrays.stream(cpString.split(File.pathSeparator))
						.map(Path::of)
						.collect(Collectors.toUnmodifiableSet());
				} else {
					LOGGER.warn("Failed to build Maven classpath from {}", () -> pom, result::getExecutionException);
				}
			} else {
				LOGGER.warn("Cannot find Maven executable; skipping classpath resolution for {}", pom);
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

		return Set.of();
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
		request.setOutputHandler(LOGGER::trace);
		request.setErrorHandler(LOGGER::warn);
		return request;
	}

	/**
	 * Attempts to retrieve some mvn executable (local wrapper > MAVEN/M2_HOME > PATH)
	 */
	private static Optional<File> findMavenExecutable(Path pom) {
		// Look for a potential wrapper
		boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
		Path mvnw = pom.resolveSibling(isWindows ? "mvnw.cmd" : "mvnw");
		if (Files.isExecutable(mvnw)) {
			return Optional.of(mvnw.toFile());
		}

		// MAVEN_HOME / M2_HOME
		for (String env : new String[]{"MAVEN_HOME", "M2_HOME"}) {
			String home = System.getenv(env);
			if (!Strings.isNullOrEmpty(home)) {
				Path bin = Path.of(home).resolve("bin").normalize();
				Path exe = bin.resolve(isWindows ? "mvn.cmd" : "mvn");
				if (Files.isExecutable(exe)) {
					return Optional.of(exe.toFile());
				}
				if (isWindows) {
					Path bat = bin.resolve("mvn.bat");
					if (Files.isExecutable(bat)) {
						return Optional.of(bat.toFile());
					}
				}
			}
		}

		// PATH
		String pathEnv = System.getenv("PATH");
		if (!Strings.isNullOrEmpty(pathEnv)) {
			String[] parts = pathEnv.split(File.pathSeparator);
			List<String> names = isWindows ? List.of("mvn.cmd", "mvn.bat") : List.of("mvn");
			for (String p : parts) {
				for (String name : names) {
					Path candidate = Path.of(p).resolve(name);
					if (Files.isExecutable(candidate)) {
						return Optional.of(candidate.toFile());
					}
				}
			}
		}

		return Optional.empty();
	}
}
