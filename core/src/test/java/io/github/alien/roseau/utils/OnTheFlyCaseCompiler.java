package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.extractors.APIExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonAPIExtractor;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

	public static void assertBC(String snippet1, String snippet2, String clientSnippet,
	                            String symbol, BreakingChangeKind kind, int line) {
		CaseResult res = roseauCase(snippet1, snippet2, clientSnippet);
		if (!res.isBinaryBreaking() && !res.isSourceBreaking())
			throw new AssertionFailedError("No breaking change detected");
		TestUtils.assertBC(symbol, kind, line, res.bcs());
	}

	public static void assertNoBC(String snippet1, String snippet2, String clientSnippet) {
		CaseResult res = roseauCase(snippet1, snippet2, clientSnippet);
		if (!res.compilationErrors().isEmpty())
			throw new AssertionFailedError("Compilation error", "No error", res.oneLineCompilationError());
		if (res.linkingError() != null)
			throw new AssertionFailedError("Linking error", "No error", res.oneLineLinkingError());
		TestUtils.assertNoBC(res.bcs());
	}

	record CaseResult(
		List<BreakingChange> bcs,
		List<Diagnostic<? extends JavaFileObject>> compilationErrors,
		Exception linkingError
	) {
		boolean isBinaryBreaking() {
			return linkingError != null;
		}

		boolean isSourceBreaking() {
			return !compilationErrors.isEmpty();
		}

		String oneLineCompilationError() {
			return compilationErrors.stream().map(d -> d.toString().replace("\n", " ")).collect(Collectors.joining(", "));
		}

		String oneLineLinkingError() {
			return linkingError != null ? linkingError.getMessage().replace("\n", " ") : "";
		}
	}

	private Path saveSource(String directory, String className, String source) throws IOException {
		Path sources = workingDirectory.resolve(directory).resolve("src");
		Path sourcePath = sources.resolve("%s.java".formatted(className));
		sources.toFile().mkdirs();
		Files.writeString(sourcePath, source);
		return sourcePath;
	}

	private void createJar(Path jar, Collection<Path> classFiles) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jar.toFile()), manifest)) {
			for (Path classFile : classFiles)
				addToJar(classFile, target);
		}
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
			"-classpath", libraryPath.toString()
		);

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, cus);
		task.call();

		return diagnostics.getDiagnostics().stream()
			.filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
			.toList();
	}

	private Exception linkClient(Path clientDir, Path libraryDir) {
		try {
			Path clientClassFile = clientDir.resolve("Client.class");
			if (!Files.exists(clientClassFile))
				throw new RuntimeException("Compiled client class not found at: " + clientClassFile);

			String classpath = clientDir + System.getProperty("path.separator") + libraryDir;

			ProcessBuilder pb = new ProcessBuilder(
				"java",
				"-cp", classpath,
				"Client"
			);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				return new RuntimeException(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
			}
			return null;
		} catch (IOException | InterruptedException e) {
			return e;
		}
	}

	private static CaseResult roseauCase(String snippet1, String snippet2, String clientSnippet) {
		try {
			Path workingDirectory = Files.createTempDirectory("roseau-otf");
			OnTheFlyCaseCompiler otf = new OnTheFlyCaseCompiler(workingDirectory);

			// --- Compile API version 1 ---
			Path srcFile1 = otf.saveSource("v1", "A", snippet1);
			Path clsFile1 = otf.compileLibrary("A", srcFile1);
			Path srcDir1 = srcFile1.getParent();
			Path clsDir1 = clsFile1.getParent();
			Path jar1 = workingDirectory.resolve("v1.jar");
			otf.createJar(jar1, List.of(clsFile1));

			// --- Compile API version 2 ---
			Path srcFile2 = otf.saveSource("v2", "A", snippet2);
			Path clsFile2 = otf.compileLibrary("A", srcFile2);
			Path srcDir2 = srcFile2.getParent();
			Path clsDir2 = clsFile2.getParent();
			Path jar2 = workingDirectory.resolve("v2.jar");
			otf.createJar(jar2, List.of(clsFile2));

			// --- Prepare client code ---
			Path clientPath = workingDirectory.resolve("client/src");
			Path clientFile = clientPath.resolve("Client.java");
			clientFile.getParent().toFile().mkdirs();
			Files.writeString(clientFile, """
				public class Client {
					public static void main(String[] args) {
						%s
					}
				}""".formatted(clientSnippet));

			// --- Extract APIs and compute diff ---
			APIExtractor extractor = new SpoonAPIExtractor();
			API v1 = extractor.extractAPI(srcDir1);
			API v2 = extractor.extractAPI(srcDir2);
			List<BreakingChange> bcs = new APIDiff(v1, v2).diff();

			// --- Compile client against API v1 (sanity check) ---
			List<Diagnostic<? extends JavaFileObject>> compilationErrors1 = otf.compileClient(clientFile, clsDir1);
			if (!compilationErrors1.isEmpty())
				throw new RuntimeException("Client did not compile against the first version:\n" +
					compilationErrors1.stream().map(d -> d.getMessage(null)).collect(Collectors.joining(System.lineSeparator())));

			// --- Link client against API V2 ---
			Exception linkingError2 = otf.linkClient(clientPath, clsDir2);

			// --- Compile client against API v2 ---
			// We do this last to avoid having a Client.class compiled against v2 above
			List<Diagnostic<? extends JavaFileObject>> compilationErrors2 = otf.compileClient(clientFile, clsDir2);

			MoreFiles.deleteRecursively(workingDirectory, RecursiveDeleteOption.ALLOW_INSECURE);
			return new CaseResult(bcs, compilationErrors2, linkingError2);
		} catch (IOException e) {
			throw new RuntimeException("On-the-fly failed", e);
		}
	}
}
