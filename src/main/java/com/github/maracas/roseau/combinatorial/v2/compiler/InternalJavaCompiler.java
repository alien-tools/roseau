package com.github.maracas.roseau.combinatorial.v2.compiler;

import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.utils.ExplorerUtils;

import javax.tools.*;
import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class InternalJavaCompiler {
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

	public List<Diagnostic<? extends JavaFileObject>> compileClientWithApi(Path clientsPath, Path apiPath, Path binPath) {
		var clientsFiles = ExplorerUtils.getFilesInPath(clientsPath, "java");
		if (!ExplorerUtils.cleanOrCreateDirectory(binPath))
			return List.of(new InternalDiagnostic("Couldn't clean or create clients binaries directory"));

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(binPath.toFile()));

			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(clientsFiles);
			List<String> options = List.of("-source", "21", "-cp", apiPath.toString());
			JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits);
			task.call();
		} catch (Exception e) {
			return List.of(new InternalDiagnostic("Unknown error while compiling clients"));
		}

		return diagnostics.getDiagnostics().stream()
				.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
				.toList();
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
}
