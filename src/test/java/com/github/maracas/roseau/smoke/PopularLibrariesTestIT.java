package com.github.maracas.roseau.smoke;

import com.github.maracas.roseau.api.SpoonUtils;
import com.github.maracas.roseau.api.extractors.SpoonAPIExtractor;
import com.github.maracas.roseau.api.extractors.jar.JarAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import spoon.reflect.CtModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PopularLibrariesTestIT {

	static Stream<String> libraries() {
		return Stream.of(
			"com.google.guava:guava:32.1.3-jre",
			"org.apache.commons:commons-lang3:3.14.0",
			"org.eclipse.collections:eclipse-collections:11.1.0",
			"org.springframework:spring-core:6.1.5",
			"io.dropwizard:dropwizard-core:4.0.1",
			"io.quarkus:quarkus-core:3.6.5",
			"io.projectreactor:reactor-core:3.6.3",
			"org.reactivestreams:reactive-streams:1.0.4",
			"org.apache.spark:spark-core_2.13:3.5.0",
			"org.apache.kafka:kafka-clients:3.6.0",
			"com.fasterxml.jackson.core:jackson-databind:2.16.1",
			"com.google.code.gson:gson:2.10.1",
			"org.junit.jupiter:junit-jupiter-api:5.10.1",
			"org.testng:testng:7.8.0",
			"org.mockito:mockito-core:5.7.0",
			"com.squareup:javapoet:1.13.0",
			"org.jooq:joor-java-8:0.9.15",
			"joda-time:joda-time:2.12.5",
			"com.google.auto.service:auto-service:1.1.1"
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("libraries")
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	void analyzeLibrary(String libraryGAV) {
		String[] parts = libraryGAV.split(":");
		String groupId = parts[0];
		String artifactId = parts[1];
		String version = parts[2];

		try {
			Path binaryJar = downloadBinaryJar(groupId, artifactId, version);
			Path sourcesJar = downloadSourcesJar(groupId, artifactId, version);
			Path sourcesDir = extractSourcesJar(sourcesJar);

			// Parse, build API, (self-)diff
			Stopwatch sw = Stopwatch.createStarted();
			CtModel model = SpoonUtils.buildModel(sourcesDir, Duration.ofMinutes(1));
			long parsingTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			SpoonAPIExtractor sourcesExtractor = new SpoonAPIExtractor();
			API sourcesApi = sourcesExtractor.extractAPI(model);
			long sourcesApiTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			JarAPIExtractor jarExtractor = new JarAPIExtractor();
			API jarApi = jarExtractor.extractAPI(binaryJar);
			long jarApiTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			APIDiff jarToSourcesDiff = new APIDiff(jarApi, sourcesApi);
			List<BreakingChange> jarToSourcesBCs = jarToSourcesDiff.diff();
			APIDiff sourcesToJarDiff = new APIDiff(sourcesApi, jarApi);
			List<BreakingChange> sourcesToJarBCs = sourcesToJarDiff.diff();
			long diffTime = sw.elapsed().toMillis();

			// Stats
			long loc = countLinesOfCode(sourcesDir);
			long numTypes = sourcesApi.getAllTypes().count();
			int numMethods = sourcesApi.getAllTypes()
				.mapToInt(type -> type.getDeclaredMethods().size())
				.sum();
			int numFields = sourcesApi.getAllTypes()
				.mapToInt(type -> type.getDeclaredFields().size())
				.sum();

			System.out.printf("Processed %s (%d LoC, %d types, %d methods, %d fields)%n" +
					"\tParsing: %dms API: %sms Diff: %dms%n" +
					"\tJAR API: %dms%n" +
					"\tJAR to Sources BCs: %d%n" +
					"\tSources to JAR BCs: %d%n",
				libraryGAV, loc, numTypes, numMethods, numFields, parsingTime, sourcesApiTime, diffTime, jarApiTime,
				sourcesToJarBCs.size(), sourcesToJarBCs.size());

			// Check everything went well
			assertFalse(sourcesApi.getAllTypes().findAny().isEmpty());
			assertTrue(jarToSourcesBCs.isEmpty(), "JAR to Sources BCs:\n" +
				jarToSourcesBCs.stream().map(BreakingChange::toString).collect(Collectors.joining("\n")));
			assertTrue(sourcesToJarBCs.isEmpty(), "Sources to JAR BCs:\n" +
				sourcesToJarBCs.stream().map(BreakingChange::toString).collect(Collectors.joining("\n")));

			cleanup(sourcesJar, sourcesDir);
		} catch (Exception e) {
			fail("Failed to process " + libraryGAV, e);
		}
	}

	private Path downloadSourcesJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-sources.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private Path downloadBinaryJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private Path download(String url) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.build();

		Path tempFile = Files.createTempFile("sources-", ".jar");
		HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

		if (response.statusCode() != 200) {
			Files.deleteIfExists(tempFile);
			throw new IOException("Failed to download JAR: HTTP " + response.statusCode());
		}

		return tempFile;
	}

	private Path extractSourcesJar(Path jarPath) throws IOException {
		Path outputDir = Files.createTempDirectory("sources-");
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				Path entryPath = outputDir.resolve(entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					Files.createDirectories(entryPath.getParent());
					try (InputStream is = jar.getInputStream(entry)) {
						Files.copy(is, entryPath);
					}
				}
			}
		}
		return outputDir;
	}

	private long countLinesOfCode(Path sourcesDir) throws IOException {
		return Files.walk(sourcesDir)
			.filter(path -> path.toString().endsWith(".java"))
			.mapToLong(path -> {
				try (BufferedReader reader = Files.newBufferedReader(path)) {
					return reader.lines().count();
				} catch (IOException e) {
					System.err.println("Failed to count lines in " + path + ": " + e.getMessage());
					return 0;
				}
			})
			.sum();
	}

	private void cleanup(Path jarPath, Path sourcesDir) throws IOException {
		Files.deleteIfExists(jarPath);
		deleteDirectory(sourcesDir);
	}

	private void deleteDirectory(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walk(path)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						System.err.println("Failed to delete " + p + ": " + e.getMessage());
					}
				});
		}
	}
}
