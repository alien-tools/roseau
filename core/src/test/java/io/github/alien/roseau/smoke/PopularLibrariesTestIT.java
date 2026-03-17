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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PopularLibrariesTestIT {
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();
	private static final Path FIXTURE_CACHE_DIR = Path.of("target", "popular-libraries-fixtures");
	private static final String LIBRARIES_RESOURCE = "/popular-libraries.tsv";
	private static final int SETUP_CONCURRENCY = Math.max(1, Runtime.getRuntime().availableProcessors());

	static Stream<String> libraries() {
		return loadLibrarySpecs().stream()
			.filter(LibrarySpec::enabled)
			.map(LibrarySpec::gav);
	}

	record Lib(Path binary, Path sources, List<Path> classpath) {
	}

	record LibrarySpec(boolean enabled, String gav, String reason) {
	}

	record Coordinates(String groupId, String artifactId, String version) {
		static Coordinates parse(String libraryGAV) {
			var parts = libraryGAV.split(":");
			return new Coordinates(parts[0], parts[1], parts[2]);
		}

		Path cacheDir() {
			return FIXTURE_CACHE_DIR
				.resolve(groupId.replace('.', '/'))
				.resolve(artifactId)
				.resolve(version);
		}

		Path binaryJar() {
			return cacheDir().resolve(artifactId + "-" + version + ".jar");
		}

		Path sourcesJar() {
			return cacheDir().resolve(artifactId + "-" + version + "-sources.jar");
		}

		Path extractedSourcesDir() {
			return cacheDir().resolve(artifactId + "-" + version + "-sources");
		}

		Path pomFile() {
			return cacheDir().resolve(artifactId + "-" + version + ".pom");
		}

		Path classpathFile() {
			return cacheDir().resolve(artifactId + "-" + version + ".classpath");
		}
	}

	static Map<String, Lib> downloaded = new ConcurrentHashMap<>();

	private static List<LibrarySpec> loadLibrarySpecs() {
		try (InputStream is = PopularLibrariesTestIT.class.getResourceAsStream(LIBRARIES_RESOURCE)) {
			if (is == null) {
				throw new IllegalStateException("Missing resource " + LIBRARIES_RESOURCE);
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				return reader.lines()
					.map(String::strip)
					.filter(line -> !line.isEmpty())
					.filter(line -> !line.startsWith("#"))
					.map(PopularLibrariesTestIT::parseLibrarySpec)
					.toList();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static LibrarySpec parseLibrarySpec(String line) {
		var parts = line.split("\t", 3);
		if (parts.length < 2) {
			throw new IllegalArgumentException("Invalid popular library entry: " + line);
		}
		var enabled = switch (parts[0]) {
			case "true" -> true;
			case "false" -> false;
			default -> throw new IllegalArgumentException("Invalid enabled flag in entry: " + line);
		};
		var reason = parts.length == 3 ? parts[2] : "";
		return new LibrarySpec(enabled, parts[1], reason);
	}

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
				? Roseau.buildAPI(new LibraryTypes(jdtTypes.getLibrary(), asmTypes.getModule(),
				new HashSet<>(jdtTypes.getAllTypes())))
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

		// Equality
		sw.reset().start();
		boolean apiEquals = asmApi.equals(jdtApi);
		long apiEqualityTime = sw.elapsed().toMillis();

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
				"\tBCs: %s%n" +
				"\tEquals: %dms%n",
			libraryGAV, loc, numTypes, numMethods, numFields,
			asmApiTime, diffTime, jdtApiTime,
			asmToAsmBCs.size(),
			apiEqualityTime);

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
		assertThat(apiEquals).isTrue();

		// No BCs
		assertThat(jdtToJdtBCs).isEmpty();
		assertThat(asmToAsmBCs).isEmpty();
		assertThat(jdtToAsmBCs).isEmpty();
		assertThat(asmToJdtBCs).isEmpty();
	}

	private static Path downloadSourcesJar(Coordinates coordinates) {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-sources.jar",
			coordinates.groupId().replace('.', '/'),
			coordinates.artifactId(),
			coordinates.version(),
			coordinates.artifactId(),
			coordinates.version()), coordinates.sourcesJar());
	}

	private static Path downloadBinaryJar(Coordinates coordinates) {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
			coordinates.groupId().replace('.', '/'),
			coordinates.artifactId(),
			coordinates.version(),
			coordinates.artifactId(),
			coordinates.version()), coordinates.binaryJar());
	}

	private static Path downloadPom(Coordinates coordinates) {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom",
			coordinates.groupId().replace('.', '/'),
			coordinates.artifactId(),
			coordinates.version(),
			coordinates.artifactId(),
			coordinates.version()), coordinates.pomFile());
	}

	private static Path download(String url, Path destination) {
		if (Files.isRegularFile(destination)) {
			return destination;
		}

		try {
			Files.createDirectories(destination.getParent());
			Path tempFile = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".part");
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.build();
			HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

			if (response.statusCode() != 200) {
				Files.deleteIfExists(tempFile);
				throw new IOException("Failed to download JAR: HTTP " + response.statusCode());
			}

			Files.move(tempFile, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			return destination;
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private Path prepareSources(Coordinates coordinates) {
		var sourceJar = downloadSourcesJar(coordinates);
		return extractSourcesJar(sourceJar, coordinates.extractedSourcesDir());
	}

	private Path extractSourcesJar(Path jarPath, Path outputDir) {
		var readyMarker = outputDir.resolve(".ready");
		if (Files.isRegularFile(readyMarker)) {
			return outputDir;
		}

		try {
			deleteRecursively(outputDir);
			Files.createDirectories(outputDir);
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
			Files.writeString(readyMarker, "ok");
			return outputDir;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static List<Path> prepareClasspath(Coordinates coordinates) {
		if (Files.isRegularFile(coordinates.classpathFile())) {
			try {
				var paths = Files.readAllLines(coordinates.classpathFile()).stream()
					.filter(line -> !line.isBlank())
					.map(Path::of)
					.toList();
				if (paths.stream().allMatch(Files::isRegularFile)) {
					return paths;
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		var pom = downloadPom(coordinates);
		var classpath = new MavenClasspathBuilder().buildClasspath(pom);
		try {
			Files.createDirectories(coordinates.classpathFile().getParent());
			Files.write(coordinates.classpathFile(), classpath.stream()
				.map(Path::toString)
				.toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return classpath;
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}

		try (var paths = Files.walk(root)) {
			paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private Lib prepareLibrary(String libraryGAV) {
		var coordinates = Coordinates.parse(libraryGAV);
		var binary = downloadBinaryJar(coordinates);
		var sources = prepareSources(coordinates);
		var classpath = prepareClasspath(coordinates);
		return new Lib(binary, sources, classpath);
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
		var libraries = libraries().toList();
		var total = libraries.size();

		System.out.printf("Preparing %d smoke-test fixtures using %d concurrent setup slots%n", total, SETUP_CONCURRENCY);
		try (var executor = Executors.newFixedThreadPool(SETUP_CONCURRENCY)) {
			var futures = IntStream.range(0, total)
				.mapToObj(index -> executor.submit(() -> {
					var libraryGAV = libraries.get(index);
					var sw = Stopwatch.createStarted();
					System.out.printf("[setup %d/%d] Preparing %s%n", index + 1, total, libraryGAV);
					var prepared = prepareLibrary(libraryGAV);
					System.out.printf("[setup %d/%d] Ready %s in %dms%n",
						index + 1,
						total,
						libraryGAV,
						sw.elapsed().toMillis());
					return Map.entry(libraryGAV, prepared);
				}))
				.toList();

			for (var future : futures) {
				var entry = future.get();
				downloaded.put(entry.getKey(), entry.getValue());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
		}
	}
}
