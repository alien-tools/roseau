package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provides type declarations by resolving fully qualified names based on a user-specified classpath.
 * <p>
 * This implementation supports searching for type declarations both on the platform class loader (prioritized) and in
 * the specified classpath entries.
 */
public class ClasspathTypeProvider implements TypeProvider {
	private final AsmTypesExtractor extractor;
	private final List<ClasspathEntry> classpath;

	private static final ClassLoader PLATFORM_CLASS_LOADER = ClassLoader.getPlatformClassLoader();
	private static final Runtime.Version RUNTIME_VERSION = JarFile.runtimeVersion();
	private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathTypeProvider.class);

	/**
	 * Constructs a {@code ClasspathTypeProvider} that resolves and provides type declarations
	 * based on the specified classpath entries.
	 *
	 * @param extractor the {@link AsmTypesExtractor} responsible for extracting type declarations from class files
	 * @param classpath a list of paths representing the classpath entries (directories or JAR files) to be searched
	 */
	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath) {
		this.extractor = Preconditions.checkNotNull(extractor);
		this.classpath = Preconditions.checkNotNull(classpath).stream()
			.map(ClasspathEntry::new)
			.toList();
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		String entryName = nameToEntry(qualifiedName);
		return readPlatformType(entryName)
			.or(() -> readClasspathType(entryName))
			.filter(type::isInstance)
			.map(type::cast);
	}

	private static String nameToEntry(String name) {
		return name.replace('.', '/') + ".class";
	}

	private Optional<TypeDecl> readPlatformType(String entryName) {
		try (InputStream in = PLATFORM_CLASS_LOADER.getResourceAsStream(entryName)) {
			return extractType(in);
		} catch (IOException e) {
			LOGGER.warn("Failed to read platform class {}: {}", entryName, e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<TypeDecl> readClasspathType(String entryName) {
		for (ClasspathEntry entry : classpath) {
			Optional<TypeDecl> resolved = entry.isDirectory()
				? readDirectoryType(entry, entryName)
				: readJarType(entry, entryName);
			if (resolved.isPresent()) {
				return resolved;
			}
		}

		return Optional.empty();
	}

	private Optional<TypeDecl> readDirectoryType(ClasspathEntry directory, String entryName) {
		Path classFile = directory.path.resolve(entryName);
		if (!Files.isRegularFile(classFile)) {
			return Optional.empty();
		}

		try (InputStream in = Files.newInputStream(classFile)) {
			return extractType(in);
		} catch (IOException e) {
			LOGGER.warn("Failed to read class file {}: {}", classFile, e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<TypeDecl> readJarType(ClasspathEntry jar, String entryName) {
		if (!jar.classFiles().contains(entryName)) {
			return Optional.empty();
		}

		try (JarFile jarFile = new JarFile(jar.path.toFile(), false, ZipFile.OPEN_READ, RUNTIME_VERSION)) {
			var entry = jarFile.getJarEntry(entryName);
			if (entry == null) {
				return Optional.empty();
			}

			try (InputStream in = jarFile.getInputStream(entry)) {
				return extractType(in);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to read class {} from classpath entry {}: {}", entryName, jar.path, e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<TypeDecl> extractType(InputStream in) throws IOException {
		if (in == null) {
			return Optional.empty();
		}

		ExtractorSink sink = new ExtractorSink(1);
		extractor.processEntry(in.readAllBytes(), sink);

		if (sink.getTypes().size() != 1) {
			return Optional.empty();
		}

		return Optional.of(sink.getTypes().iterator().next());
	}

	private static final class ClasspathEntry {
		private final Path path;
		private final Supplier<Set<String>> classFiles;

		private ClasspathEntry(Path path) {
			this.path = path;
			this.classFiles = Suppliers.memoize(() -> indexJar(path));
		}

		private static Set<String> indexJar(Path path) {
			if (!Files.isRegularFile(path)) {
				LOGGER.warn("Classpath entry {} does not exist or is not a regular file", path);
				return Set.of();
			}

			try (JarFile jarFile = new JarFile(path.toFile(), false, ZipFile.OPEN_READ, RUNTIME_VERSION)) {
				return jarFile.versionedStream()
					.map(ZipEntry::getName)
					.filter(name -> name.endsWith(".class"))
					.collect(Collectors.toUnmodifiableSet());
			} catch (IOException e) {
				LOGGER.warn("Failed to read classpath entry {}: {}", path, e.getMessage());
				return Set.of();
			}
		}

		private Set<String> classFiles() {
			return classFiles.get();
		}

		private boolean isDirectory() {
			return Files.isDirectory(path);
		}
	}
}
