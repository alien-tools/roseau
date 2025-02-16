package com.github.maracas.roseau.utils;

import com.github.maracas.roseau.extractors.APIExtractor;
import com.github.maracas.roseau.extractors.sources.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import org.opentest4j.AssertionFailedError;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class OnTheFlyCaseCompiler {
	private final Path workingDirectory;

	public OnTheFlyCaseCompiler(Path workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	private Path saveSource(String source, String className) throws IOException {
		Path sources = workingDirectory.resolve(source.toLowerCase()).resolve("src");
		Path sourcePath = sources.resolve("%s.java".formatted(className));
		sources.toFile().mkdirs();
		Files.writeString(sourcePath, source);
		return sourcePath;
	}

	private void createJar(Path jar, Collection<Path> classFiles) throws IOException {
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

	private Path compileLibrary(String className, Path sourceFile) {
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

	private List<Diagnostic<? extends JavaFileObject>> compileClient(Path clientSource, Path libraryPath) {
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

	public static CaseResult roseauCase(String snippet1, String snippet2, String clientSnippet) {
		try {
			Path workingDirectory = Files.createTempDirectory("roseau-otf");
			OnTheFlyCaseCompiler otf = new OnTheFlyCaseCompiler(workingDirectory);

			Path srcFile1 = otf.saveSource(snippet1, "A");
			Path clsFile1 = otf.compileLibrary("A", srcFile1);
			Path jar1 = workingDirectory.resolve("old.jar");
			otf.createJar(jar1, List.of(clsFile1));

			Path srcFile2 = otf.saveSource(snippet2, "A");
			Path clsFile2 = otf.compileLibrary("A", srcFile2);
			Path jar2 = workingDirectory.resolve("new.jar");
			otf.createJar(jar2, List.of(clsFile2));

			Path clientPath = workingDirectory.resolve("client");
			Path clientFile = clientPath.resolve("src/main/java/Client.java");
			clientFile.getParent().toFile().mkdirs();
			Files.writeString(clientFile, clientSnippet);
			Path pomFile = clientPath.resolve("pom.xml");
			clientFile.toFile().getParentFile().mkdirs();
			Files.writeString(pomFile, "<project></project>");

			APIExtractor extractor = new SpoonAPIExtractor();
			API v1 = extractor.extractAPI(srcFile1.getParent());
			API v2 = extractor.extractAPI(srcFile2.getParent());
			APIDiff diff = new APIDiff(v1, v2);

			List<Diagnostic<? extends JavaFileObject>> errors1 = otf.compileClient(clientFile, clsFile1.getParent());
			if (!errors1.isEmpty())
				throw new RuntimeException("On-the-fly case did not compile with the first version:\n" +
					errors1.stream().map(d -> d.getMessage(null)).collect(Collectors.joining(System.lineSeparator())));

			List<Diagnostic<? extends JavaFileObject>> errors2 = otf.compileClient(clientFile, clsFile2.getParent());
			MoreFiles.deleteRecursively(workingDirectory, RecursiveDeleteOption.ALLOW_INSECURE);
			return new CaseResult(diff.diff(), errors2);
		} catch (IOException e) {
			throw new RuntimeException("On-the-fly failed", e);
		}
	}

	record CaseResult(List<BreakingChange> bcs, List<Diagnostic<? extends JavaFileObject>> errors) {}

	public static void assertBC(String snippet1, String snippet2, String clientSnippet,
	                            String symbol, BreakingChangeKind kind, int line) {
		CaseResult res = roseauCase(snippet1, snippet2, clientSnippet);
		if (res.errors().isEmpty())
			throw new AssertionFailedError("No breaking change detected by the compiler");
		TestUtils.assertBC(symbol, kind, line, res.bcs());
	}

	public static void assertNoBC(String snippet1, String snippet2, String clientSnippet) {
		CaseResult res = roseauCase(snippet1, snippet2, clientSnippet);
		if (!res.errors().isEmpty())
			throw new AssertionFailedError("Compiler returned an error", "No error", res.errors());
		TestUtils.assertNoBC(res.bcs());
	}
}
