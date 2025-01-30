package com.github.maracas.roseau.combinatorial.v2.compiler;

import javax.tools.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class InternalJavaCompiler {
	public Path compileLibrary(String className, Path sourceFile) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> cus = fileManager.getJavaFileObjects(sourceFile);

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, cus);
		task.call();

		List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics().stream()
			.filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
			.toList();

		if (!errors.isEmpty())
			throw new RuntimeException("Couldn't compile library :" +
				errors.stream().map(Object::toString).collect(Collectors.joining(System.lineSeparator())));

		return sourceFile.getParent().resolve("%s.class".formatted(className));
	}

	public List<Diagnostic<? extends JavaFileObject>> compileClient(Path clientSource, Path libraryPath) {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> cus = fileManager.getJavaFileObjects(clientSource);
		List<String> options = List.of(
			"-source", "21",
			"--enable-preview", // Simpler main()
			"-classpath", libraryPath.toString());

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, cus);
		task.call();

		return diagnostics.getDiagnostics().stream()
			.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
			.toList();
	}

	public void createJar(Path jar, Collection<Path> classFiles) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		JarOutputStream target = new JarOutputStream(new FileOutputStream(jar.toFile()), manifest);
		for (Path classFile : classFiles)
			addToJar(classFile, target);
		target.close();
	}

	private void addToJar(Path source, JarOutputStream target) throws IOException {
		JarEntry entry = new JarEntry(source.getFileName().toString());
		entry.setTime(source.toFile().lastModified());
		target.putNextEntry(entry);
		try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source.toFile()))) {
			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1)
					break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		}
	}

//	public static CaseResult roseauCase(String snippet1, String snippet2, String clientSnippet) {
//		try {
//			Path workingDirectory = Files.createTempDirectory("roseau-otf");
//			OnTheFlyCaseCompiler otf = new OnTheFlyCaseCompiler(workingDirectory);
//
//			Path srcFile1 = otf.saveSource(snippet1, "A");
//			Path clsFile1 = otf.compileLibrary("A", srcFile1);
//			Path jar1 = workingDirectory.resolve("old.jar");
//			otf.createJar(jar1, List.of(clsFile1));
//
//			Path srcFile2 = otf.saveSource(snippet2, "A");
//			Path clsFile2 = otf.compileLibrary("A", srcFile2);
//			Path jar2 = workingDirectory.resolve("new.jar");
//			otf.createJar(jar2, List.of(clsFile2));
//
//			Path clientPath = workingDirectory.resolve("client");
//			Path clientFile = clientPath.resolve("src/main/java/Client.java");
//			clientFile.getParent().toFile().mkdirs();
//			Files.writeString(clientFile, clientSnippet);
//			Path pomFile = clientPath.resolve("pom.xml");
//			clientFile.toFile().getParentFile().mkdirs();
//			Files.writeString(pomFile, "<project></project>");
//
//			//		Files.deleteIfExists(workingDirectory);
//
//			List<Diagnostic<? extends JavaFileObject>> errors1 = otf.compileClient(clientFile, clsFile1.getParent());
//			if (!errors1.isEmpty())
//				throw new RuntimeException("On-the-fly case did not compile with the first version:\n" +
//					errors1.stream().map(d -> d.getMessage(null)).collect(Collectors.joining(System.lineSeparator())));
//
//			List<Diagnostic<? extends JavaFileObject>> errors2 = otf.compileClient(clientFile, clsFile2.getParent());
//			return new CaseResult(diff.diff(), errors2);
//		} catch (IOException e) {
//			throw new RuntimeException("On-the-fly failed", e);
//		}
//	}
}
