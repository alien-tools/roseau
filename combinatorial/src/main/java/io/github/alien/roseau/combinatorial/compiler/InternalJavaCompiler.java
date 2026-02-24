package io.github.alien.roseau.combinatorial.compiler;

import io.github.alien.roseau.combinatorial.Constants;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public final class InternalJavaCompiler {
	private static final Logger LOGGER = LogManager.getLogger(InternalJavaCompiler.class);

	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

	public List<Diagnostic<? extends JavaFileObject>> packageApiToJar(Path apiPath, Path jarPath) {
		var binPath = jarPath.getParent().resolve(Constants.BINARIES_FOLDER);

		var errors = compileApi(apiPath, binPath);
		if (!errors.isEmpty()) return errors;

		try (var target = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
			var binFile = binPath.toFile();
			addFileToJar(binFile, target, binFile.getAbsolutePath().length() + 1);
		} catch (IOException e) {
			return List.of(new InternalDiagnostic("Unknown error while packaging API to JAR"));
		}

		ExplorerUtils.removeDirectory(binPath);

		return List.of();
	}

	public List<Diagnostic<? extends JavaFileObject>> compileClientWithApi(Path clientPath, String clientFilename, Path apiJarPath, Path binPath) {
		if (!ExplorerUtils.cleanOrCreateDirectory(binPath))
			return List.of(new InternalDiagnostic("Couldn't clean or create client binary directory"));

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(binPath.toFile()));

			File clientFile = clientPath.resolve("%s.java".formatted(clientFilename)).toFile();
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(clientFile));
				var runtimeClasspath = System.getProperty("java.class.path", "");
				var compileClasspath = runtimeClasspath.isBlank()
					? apiJarPath.toString()
					: "%s%s%s".formatted(apiJarPath, File.pathSeparator, runtimeClasspath);
				List<String> options = List.of("-source", "21", "-cp", compileClasspath);
				JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
				task.call();
			} catch (Exception e) {
			return List.of(new InternalDiagnostic("Unknown error while compiling client"));
		}

		return diagnostics.getDiagnostics().stream()
				.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
				.toList();
	}

	public List<Diagnostic<? extends JavaFileObject>> linkClientWithApi(Path clientBinPath, Path apiJarPath, String clientFilename, String packageName) {
		var classPaths = "%s%s%s".formatted(clientBinPath, File.pathSeparator, apiJarPath);

		ProcessBuilder pb = new ProcessBuilder("java", "-cp", classPaths, "%s.%s".formatted(packageName, clientFilename));
		pb.redirectErrorStream(true);

		try {
			Process process = pb.start();
			int exitCode = process.waitFor();

			if (exitCode != 0) {
				return List.of(new InternalDiagnostic(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)));
			}
		} catch (InterruptedException | IOException e) {
			return List.of(new InternalDiagnostic("Unknown error while linking client"));
		}

		return List.of();
	}

	public void checkClientCompilesWithApi(Path clientSourcesPath, Path apiSourcesPath, Path clientBinPath, Path apiJarPath) {
		checkSourcesArePresent(apiSourcesPath, clientSourcesPath);

		packageApi(apiSourcesPath, apiJarPath);
		compileClient(clientSourcesPath, clientBinPath, apiJarPath);
	}

	private List<Diagnostic<? extends JavaFileObject>> compileApi(Path apiPath, Path binPath) {
		var apiFiles = ExplorerUtils.getFilesInPath(apiPath, "java");
		if (!ExplorerUtils.cleanOrCreateDirectory(binPath))
			return List.of(new InternalDiagnostic("Couldn't clean or create API binaries directory"));

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(binPath.toFile()));

			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(apiFiles);
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
			task.call();
		} catch (Exception e) {
			return List.of(new InternalDiagnostic("Unknown error while compiling API"));
		}

		return diagnostics.getDiagnostics().stream()
				.filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
				.toList();
	}

	private void addFileToJar(File file, JarOutputStream target, int basePathLength) throws IOException {
		if (file.isDirectory()) {
			for (File insideFile : Objects.requireNonNull(file.listFiles()))
				addFileToJar(insideFile, target, basePathLength);
		} else {
			var entryName = file.getAbsolutePath().substring(basePathLength);
			target.putNextEntry(new JarEntry(entryName));

			try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					target.write(buffer, 0, bytesRead);
				}
			}

			target.closeEntry();
		}
	}

	private record InternalDiagnostic(String message) implements Diagnostic<JavaFileObject> {
		@Override public Kind getKind() { return null; }
		@Override public JavaFileObject getSource() { return null; }
		@Override public long getPosition() { return 0; }
		@Override public long getStartPosition() { return 0; }
		@Override public long getEndPosition() { return 0; }
		@Override public long getLineNumber() { return 0; }
		@Override public long getColumnNumber() { return 0; }
		@Override public String getCode() { return null; }
		@Override public String getMessage(Locale locale) { return message; }
		@Override public String toString() { return message; }
	}

	private void checkSourcesArePresent(Path apiSourcesPath, Path clientSourcesPath) {
		if (!ExplorerUtils.checkPathExists(apiSourcesPath))
			throw new RuntimeException("API sources are missing");

		if (!ExplorerUtils.checkPathExists(clientSourcesPath))
			throw new RuntimeException("Client sources are missing");
	}

	private void packageApi(Path apiSourcesPath, Path apiJarPath) {
		LOGGER.info("------- Packaging API -------");

		var errors = packageApiToJar(apiSourcesPath, apiJarPath);

		if (!errors.isEmpty())
			throw new RuntimeException("Couldn't package API: " + formatCompilerErrors(errors));

		LOGGER.info("-------- API packaged -------\n");
	}

	private void compileClient(Path clientSourcePath, Path clientBinPath, Path apiJarPath) {
		LOGGER.info("------- Compiling client ------");

		var errors = compileClientWithApi(clientSourcePath, Constants.CLIENT_FILENAME, apiJarPath, clientBinPath);

		if (!errors.isEmpty())
			throw new RuntimeException("Couldn't compile client:\n" + formatCompilerErrors(errors));

		LOGGER.info("------- Client compiled -------");
	}

	private static String formatCompilerErrors(List<?> errors) {
		return errors.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator()));
	}
}
