package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import io.github.alien.roseau.JavaRelease;
import io.github.alien.roseau.RoseauException;
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
 * This implementation searches for type declarations in the JDK first, then in the specified classpath entries, as
 * clients compiled for a given Java release see them. JDK types are those of that release, read from the running JDK's
 * {@code lib/ct.sym}, so that resolution does not depend on the modules the runtime contains, and classpath JARs
 * provide the multi-release entries that release loads.
 */
public class ClasspathTypeProvider implements TypeProvider {
	private final AsmTypesExtractor extractor;
	private final List<ClasspathEntry> classpath;
	// Null when the running JDK has no usable ct.sym: JDK types then come from the running JVM's modules
	private final CtSymPlatform platform;

	private static final ClassLoader PLATFORM_CLASS_LOADER = ClassLoader.getPlatformClassLoader();
	private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathTypeProvider.class);

	/**
	 * Constructs a {@code ClasspathTypeProvider} that resolves and provides type declarations based on the specified
	 * classpath entries and the running JDK.
	 *
	 * @param extractor the {@link AsmTypesExtractor} responsible for extracting type declarations from class files
	 * @param classpath a list of paths representing the classpath entries (directories or JAR files) to be searched
	 */
	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath) {
		this(extractor, classpath, JavaRelease.host());
	}

	/**
	 * Constructs a {@code ClasspathTypeProvider} that resolves and provides type declarations based on the specified
	 * classpath entries and the given JDK release.
	 *
	 * @param extractor   the {@link AsmTypesExtractor} responsible for extracting type declarations from class files
	 * @param classpath   a list of paths representing the classpath entries (directories or JAR files) to be searched
	 * @param javaRelease the Java release (see {@link JavaRelease})
	 */
	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath, int javaRelease) {
		this(extractor, classpath, javaRelease, platformFor(javaRelease));
	}

	ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath, int javaRelease, CtSymPlatform platform) {
		this.extractor = Preconditions.checkNotNull(extractor);
		Runtime.Version version = JavaRelease.version(javaRelease);
		this.classpath = Preconditions.checkNotNull(classpath).stream()
			.map(path -> new ClasspathEntry(path, version))
			.toList();
		this.platform = platform;
	}

	private static CtSymPlatform platformFor(int javaRelease) {
		Optional<CtSymPlatform> platform = CtSymPlatform.forRelease(javaRelease);
		if (platform.isPresent()) {
			return platform.get();
		}

		if (javaRelease != JavaRelease.host()) {
			throw new RoseauException("Cannot resolve the JDK types of Java release %d: %s does not provide them"
				.formatted(javaRelease, CtSymPlatform.hostCtSym()));
		}
		LOGGER.warn("No usable lib/ct.sym in {}; resolving JDK types from the running JVM's modules instead, " +
			"which may not contain every JDK type the analyzed libraries use", System.getProperty("java.home"));
		return null;
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
		try (InputStream in = platform != null
			? platform.openClass(entryName)
			: PLATFORM_CLASS_LOADER.getResourceAsStream(entryName)) {
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

		try (JarFile jarFile = new JarFile(jar.path.toFile(), false, ZipFile.OPEN_READ, jar.version)) {
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
		extractor.processInputStream(in, sink, AsmTypesExtractor.ParsingMode.WITHOUT_LOCATIONS);

		if (sink.getTypes().size() != 1) {
			return Optional.empty();
		}

		return Optional.of(sink.getTypes().iterator().next());
	}

	private static final class ClasspathEntry {
		private final Path path;
		private final Runtime.Version version;
		private final Supplier<Set<String>> classFiles;

		private ClasspathEntry(Path path, Runtime.Version version) {
			this.path = path;
			this.version = version;
			this.classFiles = Suppliers.memoize(() -> indexJar(path, version));
		}

		private static Set<String> indexJar(Path path, Runtime.Version version) {
			if (!Files.isRegularFile(path)) {
				LOGGER.warn("Classpath entry {} does not exist or is not a regular file", path);
				return Set.of();
			}

			try (JarFile jarFile = new JarFile(path.toFile(), false, ZipFile.OPEN_READ, version)) {
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
