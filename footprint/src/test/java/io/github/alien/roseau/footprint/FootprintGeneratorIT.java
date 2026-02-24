package io.github.alien.roseau.footprint;

import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FootprintGeneratorIT {
	@Test
	void generated_footprint_compiles_links_and_runs_on_full_fixture() throws Exception {
		Path sourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-it");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(sourceTree, generatedSource, "test.footprint", "Footprint");

			Path apiBin = tempDir.resolve("api-bin");
			compileSources(allJavaSources(sourceTree), apiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			compileSources(List.of(generatedSource), clientBin, List.of("-classpath", apiBin.toString()));

			RunResult run = runMain(
				"test.footprint.Footprint",
				clientBin + System.getProperty("path.separator") + apiBin
			);

			assertEquals(0, run.exitCode(), "Footprint should run successfully on v1 fixture:\n" + run.output());
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void generated_footprint_contains_core_usage_forms() throws Exception {
		String generated = new FootprintService().generate(fixtureSourceTree(), "test.footprint", "Footprint");

		assertTrue(generated.contains(".class;"), "Expected type token usages");
		assertTrue(generated.contains("::"), "Expected method reference usages");
		assertTrue(generated.contains("::new"), "Expected constructor reference usages");
		assertTrue(generated.contains(" extends "), "Expected inheritance usages");
		assertTrue(generated.contains("new fixture.full.Service()"), "Expected interface implementation usages");
		assertTrue(generated.contains("catch (fixture.full.CheckedProblem"), "Expected checked-exception catch blocks");
		assertTrue(generated.contains("catch (java.lang.RuntimeException"), "Expected runtime-safe catch blocks");
		assertTrue(generated.contains("throws fixture.full.CheckedProblem"), "Expected throws-clause usages for checked exceptions");
		assertTrue(generated.contains("throw (fixture.full.CheckedProblem) null;"), "Expected throw usages for exception types");
		assertTrue(generated.contains("fixture.full.BaseApi"), "Expected core type references");
		assertTrue(generated.contains("fixture.full.DerivedApi"), "Expected subtype references");
		assertTrue(generated.contains("fixture.full.GenericHolder<?>"), "Expected generic parameterized type usages");
		assertTrue(generated.contains("fixture.full.GenericContract<?>"), "Expected generic interface parameterized usages");
		assertTrue(generated.contains("fixture.full.BoundedContract<? extends java.lang.Number>"),
			"Expected bounded-generic parameterized type usages");
		assertTrue(generated.contains("fixture.full.IntersectionGeneric<? extends java.lang.Number>"),
			"Expected intersection-bound generic parameterized type usages");
		assertFalse(generated.contains("fixture.full.HiddenPackageType"), "Package-private top-level types should be ignored");
	}

	@Test
	void v1_generated_footprint_fails_to_compile_on_source_breaking_v2() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-source");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/Service.java"),
				"default Number identity(Number value) {",
				"default Number identity(Number value) throws CheckedProblem {"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			CompilationResult compilation = compileSourcesWithDiagnostics(
				List.of(generatedSource),
				clientBin,
				List.of("-classpath", v2ApiBin.toString())
			);
			assertFalse(compilation.success(), "Compilation should fail on source-breaking v2");
			assertTrue(
				compilation.diagnostics().contains("unreported exception") ||
					compilation.diagnostics().contains("incompatible thrown types"),
				"Expected checked-exception compilation failure, got:\n" + compilation.diagnostics()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	@Test
	void v1_compiled_footprint_fails_to_link_on_binary_breaking_v2() throws Exception {
		Path v1SourceTree = fixtureSourceTree();
		Path tempDir = Files.createTempDirectory("roseau-footprint-v2-binary");
		try {
			Path generatedSource = tempDir.resolve("Footprint.java");
			FootprintService service = new FootprintService();
			service.generateToFile(v1SourceTree, generatedSource, "test.footprint", "Footprint");

			Path v1ApiBin = tempDir.resolve("v1-api-bin");
			compileSources(allJavaSources(v1SourceTree), v1ApiBin, List.of());

			Path clientBin = tempDir.resolve("client-bin");
			compileSources(List.of(generatedSource), clientBin, List.of("-classpath", v1ApiBin.toString()));

			Path v2SourceTree = tempDir.resolve("v2-source");
			copyRecursively(v1SourceTree, v2SourceTree);
			replaceInFile(
				v2SourceTree.resolve("fixture/full/BaseApi.java"),
				"public static int STATIC_COUNTER = 1;",
				"public static int STATIC_COUNTER_V2 = 1;"
			);

			Path v2ApiBin = tempDir.resolve("v2-api-bin");
			compileSources(allJavaSources(v2SourceTree), v2ApiBin, List.of());

			RunResult run = runMain(
				"test.footprint.Footprint",
				clientBin + System.getProperty("path.separator") + v2ApiBin
			);

			assertNotEquals(0, run.exitCode(), "Expected binary linkage failure against v2");
			assertTrue(
				run.output().contains("NoSuchFieldError") || run.output().contains("LinkageError"),
				"Expected linkage-related error output, got:\n" + run.output()
			);
		} finally {
			deleteRecursively(tempDir);
		}
	}

	private static Path fixtureSourceTree() throws URISyntaxException {
		return Path.of(
			Objects.requireNonNull(
				FootprintGeneratorIT.class.getResource("/full-api/v1"),
				"Missing full-api fixture"
			).toURI()
		);
	}

	private static List<Path> allJavaSources(Path sourceTree) throws IOException {
		try (Stream<Path> stream = Files.walk(sourceTree)) {
			return stream
				.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
				.sorted()
				.toList();
		}
	}

	private static void compileSources(List<Path> sources, Path outputDir, List<String> extraOptions) throws IOException {
		CompilationResult result = compileSourcesWithDiagnostics(sources, outputDir, extraOptions);
		if (!result.success()) {
			fail("Compilation failed:\n" + result.diagnostics());
		}
	}

	private static CompilationResult compileSourcesWithDiagnostics(List<Path> sources, Path outputDir,
	                                                               List<String> extraOptions) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull(compiler, "No system Java compiler available");

		Files.createDirectories(outputDir);
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		List<String> options = new ArrayList<>();
		options.add("--release");
		options.add("25");
		options.add("-d");
		options.add(outputDir.toString());
		options.addAll(extraOptions);

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sources);
			boolean success = Boolean.TRUE.equals(
				compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call()
			);

			String message = diagnostics.getDiagnostics().stream()
				.map(FootprintGeneratorIT::formatDiagnostic)
				.collect(Collectors.joining(System.lineSeparator()));
			return new CompilationResult(success, message);
		}
	}

	private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
		String source = diagnostic.getSource() == null ? "<no-source>" : diagnostic.getSource().toUri().toString();
		return "%s:%d:%d %s".formatted(source, diagnostic.getLineNumber(), diagnostic.getColumnNumber(),
			diagnostic.getMessage(null));
	}

	private static RunResult runMain(String mainClass, String classpath) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder("java", "-cp", classpath, mainClass);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		process.getInputStream().transferTo(output);
		int exitCode = process.waitFor();
		return new RunResult(exitCode, output.toString());
	}

	private static void copyRecursively(Path sourceRoot, Path targetRoot) throws IOException {
		try (Stream<Path> stream = Files.walk(sourceRoot)) {
			for (Path source : stream.toList()) {
				Path relative = sourceRoot.relativize(source);
				Path target = targetRoot.resolve(relative);
				if (Files.isDirectory(source)) {
					Files.createDirectories(target);
				} else {
					Files.createDirectories(target.getParent());
					Files.copy(source, target);
				}
			}
		}
	}

	private static void replaceInFile(Path file, String from, String to) throws IOException {
		String content = Files.readString(file);
		if (!content.contains(from)) {
			fail("Could not find expected text to replace in " + file + ":\n" + from);
		}
		Files.writeString(file, content.replace(from, to));
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || Files.notExists(root)) {
			return;
		}

		try (Stream<Path> walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(path -> {
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

	private record RunResult(int exitCode, String output) {
	}

	private record CompilationResult(boolean success, String diagnostics) {
	}
}
