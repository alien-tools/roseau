package io.github.alien.roseau.smoke;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.MavenClasspathBuilder;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.visit.AbstractApiVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.extractors.ExtractorType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PopularLibrariesTestIT {
	@TempDir
	static Path tempDir;

	static Stream<String> libraries() {
		return Stream.of(
			"io.github.alien-tools:roseau-core:0.4.0", // ;)
			"org.assertj:assertj-core:3.27.3",
			"com.google.guava:guava:32.1.3-jre",
			"org.eclipse.collections:eclipse-collections:13.0.0",
			"io.dropwizard:dropwizard-core:4.0.1",
			"org.reactivestreams:reactive-streams:1.0.4",
			"com.google.code.gson:gson:2.10.1",
			"org.junit.jupiter:junit-jupiter-api:5.14.0",
			"org.junit.jupiter:junit-jupiter-engine:5.14.0",
			"org.junit.jupiter:junit-jupiter-api:6.0.0",
			"org.junit.jupiter:junit-jupiter-engine:6.0.0",
			"com.squareup:javapoet:1.13.0",
			"org.jooq:joor-java-8:0.9.15",
			"joda-time:joda-time:2.12.5",
			"com.google.dagger:dagger:2.55",
			"ch.qos.logback:logback-classic:1.5.16",
			"org.slf4j:slf4j-simple:2.0.16",
			"org.slf4j:slf4j-api:2.0.16",
			"tools.jackson.core:jackson-databind:3.0.0",
			"org.apache.httpcomponents.client5:httpclient5:5.4.2",
			"fr.inria.gforge.spoon:spoon-core:11.2.0",
			"org.apache.commons:commons-lang3:3.17.0",
			"commons-codec:commons-codec:1.18.0",
			"commons-io:commons-io:2.18.0",
			"commons-logging:commons-logging:1.3.5",
			"commons-beanutils:commons-beanutils:1.10.0",
			"org.hamcrest:hamcrest:3.0",
			"com.alibaba:fastjson:2.0.54",
			"org.json:json:20250107",
			"org.apache.maven:maven-plugin-api:3.9.11",
			"org.ow2.asm:asm:9.9",
			"com.google.auto.service:auto-service:1.1.1",
			"io.reactivex.rxjava3:rxjava:3.1.12",
			"org.openjdk.jmh:jmh-core:1.37",
			"org.glassfish.jersey.core:jersey-server:3.1.11",
			"org.glassfish.jersey.core:jersey-client:3.1.11"
			//"org.osgi:org.osgi.core:6.0.0", // Missing dependencies
			//"org.mapstruct:mapstruct:1.6.3", // repeatable annotation difference between ASM and JDT
			//"org.eclipse.collections:eclipse-collections-api:13.0.0", // interface diamond conflict
			//"commons-collections:commons-collections:3.2.2", // interface diamond conflict
			//"org.apache.commons:commons-collections4:4.5.0", // interface diamond conflict
			//"io.micrometer:micrometer-core:1.15.5", // sources and JARs do not match (!= version?)
			//"com.fasterxml.jackson.core:jackson-core:2.20.0", // shaded
			//"ch.qos.logback:logback-core:1.5.16", // Multi-release JAR with different API
			//"org.apache.logging.log4j:log4j-api:2.24.3", // Multi-release JAR with different API
			//"org.apache.logging.log4j:log4j-core:2.24.3", // Multi-release JAR with different API
			//"org.hibernate.orm:hibernate-core:7.1.4.Final", // Missing dependencies
			//"io.vertx:vertx-core:5.0.4", // Missing dependencies
			//"org.quartz-scheduler:quartz:2.5.0", // jakarta/JBoss missing
			//"org.immutables:value:2.11.6", // Missing dependencies
			//"org.mockito:mockito-core:5.20.0", // Missing dependencies
			//"org.projectlombok:lombok:1.18.42", // Missing dependencies
			//"com.h2database:h2:2.3.232", // javax
			//"org.springframework:spring-core:6.1.5", // Missing dependencies
			//"org.springframework:spring-context:6.2.12", // Missing dependencies
			//"org.springframework:spring-web:6.2.2", // Missing dependencies
			//"io.projectreactor:reactor-core:3.6.3", // Missing dependencies
			//"org.apache.kafka:kafka-clients:4.1.0", // Missing dependencies
		);
	}

	record Lib(Path binary, Path sources, List<Path> classpath) {
	}

	static Map<String, Lib> downloaded = new ConcurrentHashMap<>();

	@ParameterizedTest(name = "{0}")
	@MethodSource("libraries")
	@Timeout(value = 3, unit = TimeUnit.MINUTES)
	void analyzeLibrary(String libraryGAV) {
		var lib = downloaded.get(libraryGAV);
		var binaryJar = lib.binary();
		var sourcesDir = lib.sources();
		var classpath = lib.classpath();

		var sw = Stopwatch.createUnstarted();
		var asmLibrary = Library.builder()
			.location(binaryJar)
			.classpath(classpath)
			.build();
		var jdtLibrary = Library
			.builder()
			.location(sourcesDir)
			.classpath(classpath)
			.build();

		// ASM API
		sw.reset().start();
		var asmApi = Roseau.buildAPI(asmLibrary);
		var asmTypes = asmApi.getLibraryTypes();
		long asmApiTime = sw.elapsed().toMillis();

		// JDT API
		sw.reset().start();
		var jdtApi = Roseau.buildAPI(jdtLibrary);
		long jdtApiTime = sw.elapsed().toMillis();
		var jdtTypes = jdtApi.getLibraryTypes();

		// -sources JAR often do not have module-info.java (?); use the other one
		if (!jdtTypes.getModule().equals(asmTypes.getModule())) {
			System.out.printf("Different modules: asm=%s, jdt=%s%n", asmTypes.getModule(), jdtTypes.getModule());
			jdtApi = jdtApi.getLibraryTypes().getModule().isUnnamed()
				? new LibraryTypes(jdtTypes.getLibrary(), asmTypes.getModule(),
				new HashSet<>(jdtTypes.getAllTypes())).toAPI()
				: jdtApi;
		}

		var asmVisitor = new ReferenceVisitor();
		asmVisitor.$(asmApi).visit();
		var jdtVisitor = new ReferenceVisitor();
		jdtVisitor.$(jdtApi).visit();

		if (asmVisitor.unresolved.size() + jdtVisitor.unresolved.size() > 0) {
			System.out.println("classpath=" + classpath);
			System.out.printf("Unresolved references: asm=%d jdt=%d%n",
				asmVisitor.unresolved.size(), jdtVisitor.unresolved.size());
			fail("Unresolved references");
		}

		// Diffs
		sw.reset().start();
		var asmToAsmBCs = Roseau.diff(asmApi, asmApi).getAllBreakingChanges();
		long diffTime = sw.elapsed().toMillis();
		var jdtToJdtBCs = Roseau.diff(jdtApi, jdtApi).getAllBreakingChanges();
		var asmToJdtBCs = Roseau.diff(asmApi, jdtApi).getAllBreakingChanges();
		var jdtToAsmBCs = Roseau.diff(jdtApi, asmApi).getAllBreakingChanges();

		// Stats
		long loc = countLinesOfCode(sourcesDir);
		int numTypes = jdtTypes.getAllTypes().size();
		int numMethods = jdtTypes.getAllTypes().stream()
			.mapToInt(type -> type.getDeclaredMethods().size())
			.sum();
		int numFields = jdtTypes.getAllTypes().stream()
			.mapToInt(type -> type.getDeclaredFields().size())
			.sum();

		System.out.printf("Processed %s (%d LoC, %d types, %d methods, %d fields)%n" +
				"\tASM: %dms; %dms diff%n" +
				"\tJDT: %dms%n" +
				"\tBCs: %s%n",
			libraryGAV, loc, numTypes, numMethods, numFields,
			asmApiTime, diffTime, jdtApiTime,
			asmToAsmBCs.size());

		if (!jdtTypes.getAllTypes().equals(asmApi.getLibraryTypes().getAllTypes())) {
			jdtTypes.getAllTypes().forEach(jdtType -> {
				var asmType = asmTypes.findType(jdtType.getQualifiedName()).orElseThrow(
					() -> new AssertionError("Missing ASM type: " + jdtType.getQualifiedName()));
				if (!asmType.equals(jdtType)) {
					System.out.println("jdt=" + jdtType);
					System.out.println("asm=" + asmType);
				}
			});

			fail("Type mismatch");
		}

		// We extracted some types
		assertThat(asmTypes.getAllTypes()).isNotEmpty();
		assertThat(jdtTypes.getAllTypes()).isNotEmpty();

		// Equal APIs
		assertThat(asmTypes.getAllTypes()).isEqualTo(jdtTypes.getAllTypes());
		assertThat(asmApi.getExportedTypes()).isEqualTo(jdtApi.getExportedTypes());

		// No BCs
		assertThat(jdtToJdtBCs).isEmpty();
		assertThat(asmToAsmBCs).isEmpty();
		assertThat(jdtToAsmBCs).isEmpty();
		assertThat(asmToJdtBCs).isEmpty();
	}

	private static Path downloadSourcesJar(String groupId, String artifactId, String version) {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-sources.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private static Path downloadBinaryJar(String groupId, String artifactId, String version) {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private static Path downloadPom(String groupId, String artifactId, String version) {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private static Path download(String url) {
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.build();

			String file = Long.toHexString(Double.doubleToLongBits(Math.random()));
			Path tempFile = tempDir.resolve(file);
			HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

			if (response.statusCode() != 200) {
				Files.deleteIfExists(tempFile);
				throw new IOException("Failed to download JAR: HTTP " + response.statusCode());
			}

			return tempFile;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private Path extractSourcesJar(Path jarPath) {
		try {
			Path outputDir = jarPath.resolveSibling(jarPath.getFileName() + "-extracted");
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
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
		} catch (IOException _) {
			return -1L;
		}
	}

	static class ReferenceVisitor extends AbstractApiVisitor {
		API api;
		Set<TypeReference<?>> unresolved = new HashSet<>();

		@Override
		public Visit $(API api) {
			this.api = api;
			return super.$(api);
		}

		@Override
		public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
			if (api.resolver().resolve(it).isEmpty()) {
				unresolved.add(it);
			}
			return super.typeReference(it);
		}
	}

	@BeforeAll
	void setUp() {
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var futures = libraries()
				.collect(Collectors.toMap(
					libraryGAV -> libraryGAV,
					libraryGAV -> {
						var parts = libraryGAV.split(":");
						var groupId = parts[0];
						var artifactId = parts[1];
						var version = parts[2];

						var binaryFuture = CompletableFuture
							.supplyAsync(() -> downloadBinaryJar(groupId, artifactId, version), executor);
						var sourcesFuture = CompletableFuture
							.supplyAsync(() -> downloadSourcesJar(groupId, artifactId, version), executor)
							.thenApplyAsync(this::extractSourcesJar, executor);
						var classpathFuture = CompletableFuture
							.supplyAsync(() -> downloadPom(groupId, artifactId, version), executor)
							.thenApplyAsync(pom -> new MavenClasspathBuilder().buildClasspath(pom), executor);
						return CompletableFuture.allOf(binaryFuture, sourcesFuture, classpathFuture)
							.thenApply(_ -> new Lib(binaryFuture.join(), sourcesFuture.join(), classpathFuture.join()));
					}
				));

			downloaded.putAll(futures.entrySet().stream()
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					e -> e.getValue().join()
				)));
		}
	}
}
