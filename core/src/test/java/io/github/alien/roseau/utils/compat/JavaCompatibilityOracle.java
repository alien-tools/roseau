package io.github.alien.roseau.utils.compat;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import io.github.alien.roseau.utils.Client;
import io.github.alien.roseau.utils.TestUtils;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public final class JavaCompatibilityOracle {
	private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(10);
	private static final String PATH_SEPARATOR = System.getProperty("path.separator");
	private static final String SYNTHETIC_API_PACKAGE = "api";
	private static final String CLIENT_PACKAGE = "client";

	private JavaCompatibilityOracle() {
	}

	static JavaCompatibilityResult check(DiffCompatibilityCase compatibilityCase, Client client) {
		try {
			Path workingDirectory = Files.createTempDirectory("roseau-ground-truth");
			try {
				return check(workingDirectory, compatibilityCase, client);
			} finally {
				MoreFiles.deleteRecursively(workingDirectory, RecursiveDeleteOption.ALLOW_INSECURE);
			}
		} catch (IOException e) {
			return JavaCompatibilityResult.failed("Could not prepare ground truth workspace: " + e.getMessage());
		}
	}

	private static JavaCompatibilityResult check(Path workingDirectory, DiffCompatibilityCase compatibilityCase,
	                                             Client client) throws IOException {
		SourceSet v1Sources = writeSources(workingDirectory.resolve("v1/src"), compatibilityCase.sourcesV1());
		SourceSet v2Sources = writeSources(workingDirectory.resolve("v2/src"), compatibilityCase.sourcesV2());
		Path v1Classes = workingDirectory.resolve("v1/classes");
		Path v2Classes = workingDirectory.resolve("v2/classes");

		CompilationResult v1Compilation = compileSources(v1Sources.root(), v1Classes, compatibilityCase.classpathV1());
		CompilationResult v2Compilation = compileSources(v2Sources.root(), v2Classes, compatibilityCase.classpathV2());
		if (!v1Compilation.succeeded() || !v2Compilation.succeeded()) {
			return new JavaCompatibilityResult(v1Compilation, v2Compilation,
				CompilationResult.skipped("client compilation requires compilable v1 and v2 libraries"),
				CompilationResult.skipped("client compilation requires compilable v1 and v2 libraries"),
				ExecutionResult.skipped("client execution requires compilable v1 and v2 libraries"),
				ExecutionResult.skipped("client execution requires compilable v1 and v2 libraries"));
		}

		Path v1Jar = workingDirectory.resolve("v1.jar");
		Path v2Jar = workingDirectory.resolve("v2.jar");
		createJar(v1Classes, v1Jar);
		createJar(v2Classes, v2Jar);

		Path clientSource = writeClient(workingDirectory.resolve("client/src"), client,
			v1Sources.hasSyntheticApiPackage());
		Path clientV1Classes = workingDirectory.resolve("client/classes-v1");
		Path clientV2Classes = workingDirectory.resolve("client/classes-v2");

		CompilationResult clientV1Compilation = compileSources(clientSource.getParent(), clientV1Classes,
			withLibrary(v1Jar, compatibilityCase.classpathV1()));
		if (!clientV1Compilation.succeeded()) {
			return new JavaCompatibilityResult(v1Compilation, v2Compilation, clientV1Compilation,
				CompilationResult.skipped("client did not compile against v1"),
				ExecutionResult.skipped("client did not compile against v1"),
				ExecutionResult.skipped("client did not compile against v1"));
		}

		CompilationResult clientV2Compilation = compileSources(clientSource.getParent(), clientV2Classes,
			withLibrary(v2Jar, compatibilityCase.classpathV2()));
		ExecutionResult clientV1Execution = executeClient(clientV1Classes, withLibrary(v1Jar, compatibilityCase.classpathV1()));
		ExecutionResult clientV2Execution = clientV1Execution.succeeded()
			? executeClient(clientV1Classes, withLibrary(v2Jar, compatibilityCase.classpathV2()))
			: ExecutionResult.skipped("client compiled against v1 did not run successfully against v1");

		return new JavaCompatibilityResult(v1Compilation, v2Compilation, clientV1Compilation, clientV2Compilation,
			clientV1Execution, clientV2Execution);
	}

	private static SourceSet writeSources(Path sourcesDirectory, String sources) throws IOException {
		Files.createDirectories(sourcesDirectory);
		boolean hasSyntheticApiPackage = false;
		for (Map.Entry<String, String> entry : TestUtils.buildSourcesMap(sources).entrySet()) {
			String sourceName = entry.getKey();
			String source = entry.getValue();
			if (shouldMoveToSyntheticApiPackage(sourceName)) {
				sourceName = SYNTHETIC_API_PACKAGE + "." + sourceName;
				source = "package " + SYNTHETIC_API_PACKAGE + ";" + System.lineSeparator() + System.lineSeparator() + source;
				hasSyntheticApiPackage = true;
			}
			Path sourceFile = sourcePath(sourcesDirectory, sourceName);
			Files.createDirectories(sourceFile.getParent());
			Files.writeString(sourceFile, source);
		}
		return new SourceSet(sourcesDirectory, hasSyntheticApiPackage);
	}

	private static boolean shouldMoveToSyntheticApiPackage(String sourceName) {
		return !"module-info".equals(sourceName) && !sourceName.contains(".");
	}

	private static Path sourcePath(Path sourcesDirectory, String sourceName) {
		if ("module-info".equals(sourceName)) {
			return sourcesDirectory.resolve("module-info.java");
		}
		return sourcesDirectory.resolve(sourceName.replace('.', '/') + ".java");
	}

	private static Path writeClient(Path sourcesDirectory, Client client, boolean importSyntheticApiPackage) throws IOException {
		Path clientPackageDirectory = sourcesDirectory.resolve(CLIENT_PACKAGE);
		Files.createDirectories(clientPackageDirectory);
		String run = client.run().isBlank() ? "" : System.lineSeparator() + client.run();
		String imports = importSyntheticApiPackage
			? "import " + SYNTHETIC_API_PACKAGE + ".*;" + System.lineSeparator() + System.lineSeparator()
			: "";
		String source = """
			package %s;

			%s\
			public class Client {
				public static void main(String[] args) {
			%s
				}
			}
			""".formatted(CLIENT_PACKAGE, imports, indent(client.value() + run));
		Path sourceFile = clientPackageDirectory.resolve("Client.java");
		Files.writeString(sourceFile, source);
		return sourceFile;
	}

	private static String indent(String source) {
		return source.lines()
			.map(line -> "\t\t\t" + line)
			.reduce((left, right) -> left + System.lineSeparator() + right)
			.orElse("");
	}

	private static CompilationResult compileSources(Path sourcesDirectory, Path classesDirectory, List<Path> classpath)
		throws IOException {
		Files.createDirectories(classesDirectory);
		List<Path> sourceFiles = listSourceFiles(sourcesDirectory);
		if (sourceFiles.isEmpty()) {
			return CompilationResult.success();
		}

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			return CompilationResult.failure("No system Java compiler is available. Run tests with a JDK, not a JRE.");
		}

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
			List<String> options = compilerOptions(classesDirectory, classpath);
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null,
				fileManager.getJavaFileObjectsFromPaths(sourceFiles));
			boolean succeeded = Boolean.TRUE.equals(task.call());
			return new CompilationResult(true, succeeded, diagnostics(diagnostics));
		}
	}

	private static List<Path> listSourceFiles(Path sourcesDirectory) throws IOException {
		try (Stream<Path> paths = Files.walk(sourcesDirectory)) {
			return paths
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".java"))
				.sorted()
				.toList();
		}
	}

	private static List<String> compilerOptions(Path classesDirectory, List<Path> classpath) {
		List<String> options = new ArrayList<>(List.of(
			"-d", classesDirectory.toString(),
			"-proc:none",
			"--release", Integer.toString(Runtime.version().feature())
		));
		if (!classpath.isEmpty()) {
			options.add("-classpath");
			options.add(joinPaths(classpath));
		}
		return options;
	}

	private static List<String> diagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
		return diagnostics.getDiagnostics().stream()
			.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
			.map(JavaCompatibilityOracle::formatDiagnostic)
			.toList();
	}

	private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
		String location = diagnostic.getSource() == null
			? ""
			: Path.of(diagnostic.getSource().toUri()).getFileName() + ":" + diagnostic.getLineNumber() + ": ";
		return location + diagnostic.getMessage(Locale.ROOT).replace(System.lineSeparator(), " ");
	}

	private static List<Path> withLibrary(Path library, List<Path> classpath) {
		List<Path> fullClasspath = new ArrayList<>(classpath.size() + 1);
		fullClasspath.add(library);
		fullClasspath.addAll(classpath);
		return List.copyOf(fullClasspath);
	}

	private static ExecutionResult executeClient(Path clientClasses, List<Path> classpath) {
		try {
			List<Path> runtimeClasspath = new ArrayList<>(classpath.size() + 1);
			runtimeClasspath.add(clientClasses);
			runtimeClasspath.addAll(classpath);

			ProcessBuilder processBuilder = new ProcessBuilder(
				Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-cp", joinPaths(runtimeClasspath),
				CLIENT_PACKAGE + ".Client"
			);
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			boolean completed = process.waitFor(EXECUTION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			if (!completed) {
				process.destroyForcibly();
				return ExecutionResult.failure(-1, "Timed out after " + EXECUTION_TIMEOUT);
			}
			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			return new ExecutionResult(true, process.exitValue() == 0, process.exitValue(), output);
		} catch (IOException e) {
			return ExecutionResult.failure(-1, e.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ExecutionResult.failure(-1, e.getMessage());
		}
	}

	private static String joinPaths(List<Path> paths) {
		return paths.stream().map(Path::toString).reduce((left, right) -> left + PATH_SEPARATOR + right).orElse("");
	}

	private static void createJar(Path classesDirectory, Path jar) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
			for (Path classFile : listClassFiles(classesDirectory)) {
				JarEntry entry = new JarEntry(classesDirectory.relativize(classFile).toString().replace('\\', '/'));
				output.putNextEntry(entry);
				Files.copy(classFile, output);
				output.closeEntry();
			}
		}
	}

	private static List<Path> listClassFiles(Path classesDirectory) throws IOException {
		if (!Files.exists(classesDirectory)) {
			return List.of();
		}

		try (Stream<Path> paths = Files.walk(classesDirectory)) {
			return paths
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".class"))
				.sorted(Comparator.comparing(Path::toString))
				.toList();
		}
	}

	private record SourceSet(Path root, boolean hasSyntheticApiPackage) {
	}

	record JavaCompatibilityResult(
		CompilationResult v1Compilation,
		CompilationResult v2Compilation,
		CompilationResult clientV1Compilation,
		CompilationResult clientV2Compilation,
		ExecutionResult clientV1Execution,
		ExecutionResult clientV2Execution
	) {
		static JavaCompatibilityResult failed(String message) {
			return new JavaCompatibilityResult(
				CompilationResult.failure(message),
				CompilationResult.skipped(message),
				CompilationResult.skipped(message),
				CompilationResult.skipped(message),
				ExecutionResult.skipped(message),
				ExecutionResult.skipped(message)
			);
		}

		boolean setupSucceeded() {
			return v1Compilation.succeeded() && v2Compilation.succeeded() && clientV1Compilation.succeeded();
		}

		boolean observedSourceBreaking() {
			return clientV2Compilation.failed();
		}

		boolean observedBinaryBreaking() {
			return clientV1Execution.succeeded() && clientV2Execution.failed();
		}
	}

	record CompilationResult(boolean ran, boolean succeeded, List<String> diagnostics) {
		CompilationResult {
			diagnostics = List.copyOf(diagnostics);
		}

		static CompilationResult success() {
			return new CompilationResult(true, true, List.of());
		}

		static CompilationResult failure(String diagnostic) {
			return new CompilationResult(true, false, List.of(diagnostic));
		}

		static CompilationResult skipped(String reason) {
			return new CompilationResult(false, false, List.of(reason));
		}

		boolean failed() {
			return ran && !succeeded;
		}
	}

	record ExecutionResult(boolean ran, boolean succeeded, int exitCode, String output) {
		static ExecutionResult failure(int exitCode, String output) {
			return new ExecutionResult(true, false, exitCode, output == null ? "" : output);
		}

		static ExecutionResult skipped(String reason) {
			return new ExecutionResult(false, false, -1, reason);
		}

		boolean failed() {
			return ran && !succeeded;
		}
	}
}
