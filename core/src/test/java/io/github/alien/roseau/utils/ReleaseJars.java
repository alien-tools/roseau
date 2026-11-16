package io.github.alien.roseau.utils;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Compiles sources with {@code javac --release} and packs class files into regular or multi-release JARs, to build
 * fixtures whose class file versions and multi-release layouts are those of real libraries.
 */
public final class ReleaseJars {
	private ReleaseJars() {

	}

	/**
	 * Compiles the given sources with {@code javac --release <release>}.
	 *
	 * @param outDir    the directory to write sources and class files to
	 * @param release   the {@code --release} to compile with
	 * @param classpath the compilation classpath
	 * @param sources   the sources, keyed by fully qualified type name, or {@code module-info}
	 * @return the directory holding the class files
	 */
	public static Path compile(Path outDir, int release, List<Path> classpath, Map<String, String> sources) {
		try {
			Path srcDir = Files.createDirectories(outDir.resolve("src"));
			Path classesDir = Files.createDirectories(outDir.resolve("classes"));
			List<Path> files = new ArrayList<>();
			for (Map.Entry<String, String> source : sources.entrySet()) {
				Path file = srcDir.resolve(source.getKey().replace('.', '/') + ".java");
				Files.createDirectories(file.getParent());
				files.add(Files.writeString(file, source.getValue()));
			}

			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
				Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(files);
				List<String> options = new ArrayList<>(List.of("--release", Integer.toString(release), "-proc:none",
					"-Xlint:-options", "-d", classesDir.toString()));
				if (!classpath.isEmpty()) {
					options.addAll(List.of("-cp", String.join(java.io.File.pathSeparator,
						classpath.stream().map(Path::toString).toList())));
				}
				Boolean success = compiler.getTask(null, fileManager, null, options, null, units).call();
				if (!Boolean.TRUE.equals(success)) {
					throw new IllegalStateException("Compilation failed for --release " + release + ": " + sources.keySet());
				}
			}
			return classesDir;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @param classesDir a directory of class files
	 * @param prefix     the prefix of their JAR entries, e.g. {@code META-INF/versions/11/}, or empty
	 * @return the class files of the directory, keyed by JAR entry name
	 */
	public static Map<String, Path> entries(Path classesDir, String prefix) {
		try (Stream<Path> files = Files.walk(classesDir)) {
			Map<String, Path> entries = new LinkedHashMap<>();
			files.filter(Files::isRegularFile)
				.sorted()
				.forEach(file -> entries.put(prefix + classesDir.relativize(file).toString().replace('\\', '/'), file));
			return entries;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Writes a JAR with the given entries.
	 *
	 * @param jar          the JAR to write
	 * @param multiRelease whether to declare {@code Multi-Release: true} in the manifest
	 * @param entries      the files to pack, keyed by JAR entry name
	 * @return the JAR
	 */
	public static Path jar(Path jar, boolean multiRelease, Map<String, Path> entries) {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		if (multiRelease) {
			manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
		}

		try (OutputStream out = Files.newOutputStream(jar); JarOutputStream jarOut = new JarOutputStream(out, manifest)) {
			for (Map.Entry<String, Path> entry : entries.entrySet()) {
				jarOut.putNextEntry(new JarEntry(entry.getKey()));
				jarOut.write(Files.readAllBytes(entry.getValue()));
				jarOut.closeEntry();
			}
			return jar;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
