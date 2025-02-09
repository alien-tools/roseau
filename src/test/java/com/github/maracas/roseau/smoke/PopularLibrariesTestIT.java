package com.github.maracas.roseau.smoke;

import com.github.maracas.roseau.api.SpoonUtils;
import com.github.maracas.roseau.api.extractors.SpoonAPIExtractor;
import com.github.maracas.roseau.api.extractors.jar.JarAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
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
import java.util.Objects;
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
			// Needs eclipse-collections-api
			// "org.eclipse.collections:eclipse-collections:11.1.0",
			"org.eclipse.collections:eclipse-collections-api:11.1.0",
			"org.springframework:spring-core:6.1.5",
			"io.dropwizard:dropwizard-core:4.0.1",
			// Needs org.graalvm.nativeimage
			// "io.quarkus:quarkus-core:3.6.5",
			"io.projectreactor:reactor-core:3.6.3",
			"org.reactivestreams:reactive-streams:1.0.4",
			"org.apache.kafka:kafka-clients:3.6.0",
			// Needs com.fasterxml.jackson.annotation
			// "com.fasterxml.jackson.core:jackson-databind:2.16.1",
			"com.google.code.gson:gson:2.10.1",
			"org.junit.jupiter:junit-jupiter-api:5.10.1",
			// Needs Google Guice
			// "org.testng:testng:7.8.0",
			// Contains *one* weird .raw file that should be a .class file?
			// "org.mockito:mockito-core:5.6.0",
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
			JarAPIExtractor jarExtractor = new JarAPIExtractor();
			API jarApi = jarExtractor.extractAPI(binaryJar);
			long jarApiTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			CtModel model = SpoonUtils.buildModel(sourcesDir, Duration.ofMinutes(1));
			long parsingTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			SpoonAPIExtractor sourcesExtractor = new SpoonAPIExtractor();
			API sourcesApi = sourcesExtractor.extractAPI(model);
			long sourcesApiTime = sw.elapsed().toMillis();
			sw.reset();
			sw.start();

			System.out.println("jarvssources");
			diffAPIs(jarApi, sourcesApi);
			System.out.println("sourcesvsjar");
			diffAPIs(sourcesApi, jarApi);

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
				jarToSourcesBCs.size(), sourcesToJarBCs.size());

			System.out.println("JAR to Sources BCs:");
			System.out.println(jarToSourcesBCs.stream()
				.map(BreakingChange::toString).collect(Collectors.joining("\n")));
			System.out.println("Sources to JAR BCs:");
			System.out.println(sourcesToJarBCs.stream()
				.map(BreakingChange::toString).collect(Collectors.joining("\n")));

			cleanup(sourcesJar);
			cleanup(binaryJar);
			cleanup(sourcesDir);

			// Check everything went well
			assertFalse(sourcesApi.getAllTypes().findAny().isEmpty());
			assertTrue(jarToSourcesBCs.isEmpty());
			assertTrue(sourcesToJarBCs.isEmpty());
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

	private void cleanup(Path path) throws IOException {
		if (path.toFile().isDirectory())
			deleteDirectory(path);
		else
			Files.deleteIfExists(path);
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

	static void diffAPIs(API api1, API api2) {
		api1.getAllTypes().forEach(t1 -> {
			var optT2 = api2.findType(t1.getQualifiedName());

			optT2.ifPresentOrElse(t2 -> {
				System.out.println("###" + t1.getQualifiedName());

				if (!t1.getClass().equals(t2.getClass()))
					System.out.printf("\t%s != %s%n", t1, t2);

				if (!t1.getModifiers().equals(t2.getModifiers()))
					System.out.printf("\t%s[%s] %s != %s%n", t1.getQualifiedName(), t1.isEnum(), t1.getModifiers(), t2.getModifiers());

				if (t1.getVisibility() != t2.getVisibility())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getVisibility(), t2.getVisibility());

				if (t1.getFormalTypeParameters().size() != t2.getFormalTypeParameters().size())
					System.out.printf("\t%s != %s%n", t1.getFormalTypeParameters(), t2.getFormalTypeParameters());
				for (int i = 0; i < t1.getFormalTypeParameters().size(); i++)
					if (!t1.getFormalTypeParameters().get(i).equals(t2.getFormalTypeParameters().get(i)))
						System.out.printf("\t%s != %s%n", t1.getFormalTypeParameters().get(i), t2.getFormalTypeParameters().get(i));

				if (t1.getImplementedInterfaces().size() != t2.getImplementedInterfaces().size())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getImplementedInterfaces(), t2.getImplementedInterfaces());
				for (int i = 0; i < t1.getImplementedInterfaces().size(); i++)
					if (!t1.getImplementedInterfaces().get(i).equals(t2.getImplementedInterfaces().get(i)))
						System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getImplementedInterfaces().get(i), t2.getImplementedInterfaces().get(i));

				if (t1.getDeclaredFields().size() != t2.getDeclaredFields().size())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredFields(), t2.getDeclaredFields());
				t1.getDeclaredFields().forEach(f1 -> {
					if (!t2.getDeclaredFields().contains(f1)) {
						System.out.printf("\tNo match for field %s: %s%n", f1, t2.getDeclaredFields());
					}
				});

				if (t1.getDeclaredMethods().size() != t2.getDeclaredMethods().size())
					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredMethods(), t2.getDeclaredMethods());
				t1.getDeclaredMethods().forEach(m1 -> {
					if (!t2.getDeclaredMethods().stream().anyMatch(m2 -> {
						if (!Objects.equals(m1.getQualifiedName(), m2.getQualifiedName()))
							return false;
						if (!Objects.equals(m1.getFormalTypeParameters(), m2.getFormalTypeParameters()))
							return false;
						if (!Objects.equals(m1.getParameters().stream().map(ParameterDecl::type).toList(), m2.getParameters().stream().map(ParameterDecl::type).toList()))
							return false;
						if (!Objects.equals(m1.getType(), m2.getType()))
							return false;
						if (!Objects.equals(m1.getVisibility(), m2.getVisibility()))
							return false;
						if (!Objects.equals(m1.getModifiers(), m2.getModifiers()))
							return false;
						if (!Objects.equals(m1.getThrownExceptions(), m2.getThrownExceptions()))
							return false;
						return true;
					})) {
						System.out.printf("\tNo match for method %s: %s%n", m1, t2.getDeclaredMethods());
					}
				});

				//				if (t1.getDeclaredMethods().size() != t2.getDeclaredMethods().size())
//					System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredMethods(), t2.getDeclaredMethods());
//				for (int i = 0; i < t1.getDeclaredMethods().size(); i++)
//					if (!t1.getDeclaredMethods().get(i).equals(t2.getDeclaredMethods().get(i)))
//						System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), t1.getDeclaredMethods().get(i), t2.getDeclaredMethods().get(i));

				if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2) {
					var sup1 = c1.getSuperClass();
					var sup2 = c2.getSuperClass();

					if (sup1.isPresent() != sup2.isPresent())
						System.out.printf("\t%s %s != %s%n", c1.getQualifiedName(), sup1, sup2);
					if (sup1.isPresent()) {
						if (!sup1.get().equals(sup2.get())) {
							System.out.printf("\t%s %s != %s%n", c1.getQualifiedName(), sup1.get(), sup2.get());
						}
					}

					if (c1.getConstructors().size() != c2.getConstructors().size())
						System.out.printf("\t%s %s != %s%n", t1.getQualifiedName(), c1.getConstructors(), c2.getConstructors());
					c1.getConstructors().forEach(cons1 -> {
						if (!c2.getConstructors().stream().anyMatch(cons2 -> {
							if (!Objects.equals(cons1.getQualifiedName(), cons2.getQualifiedName()))
								return false;
							if (!Objects.equals(cons1.getFormalTypeParameters(), cons2.getFormalTypeParameters()))
								return false;
							if (!Objects.equals(cons1.getParameters().stream().map(ParameterDecl::type).toList(), cons2.getParameters().stream().map(ParameterDecl::type).toList()))
								return false;
							if (!Objects.equals(cons1.getType(), cons2.getType()))
								return false;
							if (!Objects.equals(cons1.getVisibility(), cons2.getVisibility()))
								return false;
							if (!Objects.equals(cons1.getModifiers(), cons2.getModifiers()))
								return false;
							if (!Objects.equals(cons1.getThrownExceptions(), cons2.getThrownExceptions()))
								return false;
							return true;
						})) {
							System.out.printf("\tNo match for constructor %s: %s%n", cons1, c2.getConstructors());
						}
					});
				}
			}, () -> System.out.printf("%s not found%n", t1.getQualifiedName()));
		});
	}
}
