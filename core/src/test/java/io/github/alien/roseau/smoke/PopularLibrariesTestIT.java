package io.github.alien.roseau.smoke;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.extractors.ExtractorType;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PopularLibrariesTestIT {
	static Stream<String> libraries() {
		return Stream.of(
			"org.assertj:assertj-core:3.27.3",
			"commons-codec:commons-codec:1.18.0",
			"com.google.guava:guava:32.1.3-jre",
			"org.apache.commons:commons-lang3:3.17.0",
			"commons-io:commons-io:2.18.0",
			"org.eclipse.collections:eclipse-collections-api:11.1.0",
			"org.springframework:spring-core:6.1.5",
			"io.dropwizard:dropwizard-core:4.0.1",
			"io.projectreactor:reactor-core:3.6.3",
			"org.reactivestreams:reactive-streams:1.0.4",
			"org.apache.kafka:kafka-clients:3.6.0",
			"com.google.code.gson:gson:2.10.1",
			"org.junit.jupiter:junit-jupiter-api:5.10.1",
			"com.squareup:javapoet:1.13.0",
			"org.jooq:joor-java-8:0.9.15",
			"joda-time:joda-time:2.12.5",
			"com.google.auto.service:auto-service:1.1.1",
			"com.google.dagger:dagger:2.55",
			"ch.qos.logback:logback-core:1.5.16",
			"ch.qos.logback:logback-classic:1.5.16",
			"org.apache.logging.log4j:log4j-core:2.24.3", // external libs
			"org.apache.logging.log4j:log4j-api:2.24.3",
			"org.slf4j:slf4j-simple:2.0.16",
			"org.slf4j:slf4j-api:2.0.16",
			"com.fasterxml.jackson.core:jackson-core:2.18.2", // shaded
			"org.apache.httpcomponents.client5:httpclient5:5.4.2",
			"fr.inria.gforge.spoon:spoon-core:11.2.0",
			"commons-logging:commons-logging:1.3.5",
			"org.springframework:spring-web:6.2.2",
			"com.h2database:h2:2.3.232",
			"org.hamcrest:hamcrest:3.0",
			"org.springframework:spring-beans:6.2.2",
			"org.osgi:org.osgi.core:6.0.0",
			"com.alibaba:fastjson:2.0.54",
			"commons-collections:commons-collections:3.2.2",
			"org.json:json:20250107",
			"commons-beanutils:commons-beanutils:1.10.0",
			"com.squareup.retrofit2:retrofit:2.11.0"
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("libraries")
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	void analyzeLibrary(String libraryGAV) {
		var sw = Stopwatch.createUnstarted();
		var binaryJar = binaryJars.get(libraryGAV);
		var sourcesDir = sourcesDirs.get(libraryGAV);
		var classpath = classpaths.get(libraryGAV).stream().collect(Collectors.toSet());
		var asmLibrary = Library.builder()
			.location(binaryJar)
			.classpath(classpath)
			.extractorType(ExtractorType.ASM)
			.build();
		var spoonLibrary = Library
			.builder()
			.location(sourcesDir)
			.classpath(classpath)
			.extractorType(ExtractorType.SPOON)
			.build();
		var jdtLibrary = Library
			.builder()
			.location(sourcesDir)
			.classpath(classpath)
			.extractorType(ExtractorType.JDT)
			.build();

		// ASM API
		sw.reset().start();
		var asmApi = Roseau.buildAPI(asmLibrary);
		long asmApiTime = sw.elapsed().toMillis();

		// JDT API
		sw.reset().start();
		var jdtApi = Roseau.buildAPI(jdtLibrary);
		long jdtApiTime = sw.elapsed().toMillis();

		// Spoon API
		sw.reset().start();
		var spoonApi = Roseau.buildAPI(spoonLibrary);
		long spoonApiTime = sw.elapsed().toMillis();

		// Diffs
		var asmToJdtBCs = Roseau.diff(asmApi, jdtApi).getAllBreakingChanges();
		var jdtToAsmBCs = Roseau.diff(jdtApi, asmApi).getAllBreakingChanges();
		var spoonToSpoonBCs = Roseau.diff(spoonApi, spoonApi).getAllBreakingChanges();
		var jdtToJdtBCs = Roseau.diff(jdtApi, jdtApi).getAllBreakingChanges();
		sw.reset().start();
		var asmToAsmBCs = Roseau.diff(asmApi, asmApi).getAllBreakingChanges();
		long diffTime = sw.elapsed().toMillis();

		// Stats
		long loc = countLinesOfCode(sourcesDir);
		int numTypes = spoonApi.getLibraryTypes().getAllTypes().size();
		int numMethods = spoonApi.getLibraryTypes().getAllTypes().stream()
			.mapToInt(type -> type.getDeclaredMethods().size())
			.sum();
		int numFields = spoonApi.getLibraryTypes().getAllTypes().stream()
			.mapToInt(type -> type.getDeclaredFields().size())
			.sum();

		System.out.printf("Processed %s (%d LoC, %d types, %d methods, %d fields)%n" +
				"\tSpoon: %dms%n" +
				"\tASM: %dms; %dms diff%n" +
				"\tJDT: %dms%n" +
				"\tBCs: %s%n",
			libraryGAV, loc, numTypes, numMethods, numFields,
			spoonApiTime, asmApiTime, diffTime, jdtApiTime,
			asmToAsmBCs.size());

		if (!jdtToAsmBCs.isEmpty() || !asmToJdtBCs.isEmpty()) {
			System.out.println("JDT to ASM BCs:");
			System.out.println(jdtToAsmBCs.stream()
				.map(BreakingChange::toString).collect(Collectors.joining("\n")));
			System.out.println("ASM to JDT BCs:");
			System.out.println(asmToJdtBCs.stream()
				.map(BreakingChange::toString).collect(Collectors.joining("\n")));
		}

		// Check everything went well
		assertFalse(spoonApi.getLibraryTypes().getAllTypes().stream().findAny().isEmpty());
		assertFalse(asmApi.getLibraryTypes().getAllTypes().stream().findAny().isEmpty());
		assertFalse(jdtApi.getLibraryTypes().getAllTypes().stream().findAny().isEmpty());
		assertEquals(0, spoonToSpoonBCs.size());
		assertEquals(0, jdtToJdtBCs.size());
		assertEquals(0, asmToAsmBCs.size());
	}

	private static Path downloadSourcesJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-sources.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private static Path downloadBinaryJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private static Path downloadPom(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private static Path download(String url) throws IOException, InterruptedException {
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

	private static Path extractSourcesJar(Path jarPath) throws IOException {
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

	private long countLinesOfCode(Path sourcesDir) {
		try (Stream<Path> files = Files.walk(sourcesDir)) {
			return files
				.filter(path -> path.toString().endsWith(".java"))
				.mapToLong(path -> {
					try (BufferedReader reader = Files.newBufferedReader(path)) {
						return reader.lines().count();
					} catch (IOException e) {
						System.err.println("Failed to count lines in " + path + ": " + e.getMessage());
						return 0L;
					}
				})
				.sum();
		} catch (IOException ignored) {
			return -1L;
		}
	}

	private static void cleanup(Path path) throws IOException {
		if (path.toFile().isDirectory())
			deleteDirectory(path);
		else
			Files.deleteIfExists(path);
	}

	private static void deleteDirectory(Path path) throws IOException {
		if (Files.exists(path)) {
			try (Stream<Path> files = Files.walk(path)) {
				files.sorted(Comparator.reverseOrder())
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

	private static final Map<String, Path> binaryJars = new HashMap<>();
	private static final Map<String, Path> sourcesDirs = new HashMap<>();
	private static final Multimap<String, Path> classpaths = ArrayListMultimap.create();

	@BeforeAll
	static void setUp() {
		// Way too noisy 'cause of missing classpath
		Configurator.setLevel("spoon", Level.ERROR);
		Configurator.setLevel("io.github.alien.roseau", Level.ERROR);

		// Prepare data for all tests
		libraries().parallel().forEach(libraryGAV -> {
			try {
				String[] parts = libraryGAV.split(":");
				String groupId = parts[0];
				String artifactId = parts[1];
				String version = parts[2];

				Path binaryJar = downloadBinaryJar(groupId, artifactId, version);
				Path sourcesJar = downloadSourcesJar(groupId, artifactId, version);
				Path pom = downloadPom(groupId, artifactId, version);
				Path sourcesDir = extractSourcesJar(sourcesJar);

				try {
					Set<Path> classpath = new MavenClasspathBuilder().buildClasspath(pom);
					classpaths.putAll(libraryGAV, classpath);
				} catch (Exception e) {
					e.printStackTrace();
				}

				binaryJars.put(libraryGAV, binaryJar);
				sourcesDirs.put(libraryGAV, sourcesDir);

				cleanup(sourcesJar);
				cleanup(pom);
			} catch (Exception e) {
				throw new RuntimeException("Failed to download " + libraryGAV, e);
			}
		});
	}

	@AfterAll
	void tearDown() {
		binaryJars.values().forEach(path -> {
			try {
				cleanup(path);
			} catch (Exception ignored) {
			}
		});
		sourcesDirs.values().forEach(path -> {
			try {
				cleanup(path);
			} catch (Exception ignored) {
			}
		});
	}
}
