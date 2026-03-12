package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * Provides type declarations by resolving fully qualified names based on a user-specified classpath.
 * <p>
 * This implementation supports searching for type declarations both on the platform class loader (prioritized) and in
 * the specified classpath entries.
 */
public class ClasspathTypeProvider implements TypeProvider {
	private final AsmTypesExtractor extractor;
	private final List<Path> classpath;

	private static final ClassLoader PLATFORM_CLASS_LOADER = ClassLoader.getPlatformClassLoader();
	private static final Runtime.Version RUNTIME_VERSION = JarFile.runtimeVersion();

	/**
	 * Constructs a {@code ClasspathTypeProvider} that resolves and provides type declarations
	 * based on the specified classpath entries.
	 *
	 * @param extractor the {@link AsmTypesExtractor} responsible for extracting type declarations from class files
	 * @param classpath a list of paths representing the classpath entries (directories or JAR files) to be searched
	 */
	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath) {
		this.extractor = Preconditions.checkNotNull(extractor);
		this.classpath = List.copyOf(Preconditions.checkNotNull(classpath));
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		String entryName = nameToEntry(qualifiedName);
		return readPlatformType(entryName, type)
			.or(() -> readClasspathType(entryName, type));
	}

	private static String nameToEntry(String name) {
		return name.replace('.', '/') + ".class";
	}

	private <T extends TypeDecl> Optional<T> readPlatformType(String entryName, Class<T> type) {
		try (InputStream in = PLATFORM_CLASS_LOADER.getResourceAsStream(entryName)) {
			return extractType(in, type);
		} catch (IOException _) {
			return Optional.empty();
		}
	}

	private <T extends TypeDecl> Optional<T> readClasspathType(String entryName, Class<T> type) {
		for (Path entry : classpath) {
			Optional<T> resolved = Files.isDirectory(entry)
				? readDirectoryType(entry, entryName, type)
				: readJarType(entry, entryName, type);
			if (resolved.isPresent()) {
				return resolved;
			}
		}

		return Optional.empty();
	}

	private <T extends TypeDecl> Optional<T> readDirectoryType(Path directory, String entryName, Class<T> type) {
		Path classFile = directory.resolve(entryName);
		if (!Files.isRegularFile(classFile)) {
			return Optional.empty();
		}

		try (InputStream in = Files.newInputStream(classFile)) {
			return extractType(in, type);
		} catch (IOException _) {
			return Optional.empty();
		}
	}

	private <T extends TypeDecl> Optional<T> readJarType(Path jar, String entryName, Class<T> type) {
		if (!Files.isRegularFile(jar)) {
			return Optional.empty();
		}

		try (JarFile jarFile = new JarFile(jar.toFile(), false, ZipFile.OPEN_READ, RUNTIME_VERSION)) {
			var entry = jarFile.getJarEntry(entryName);
			if (entry == null) {
				return Optional.empty();
			}

			try (InputStream in = jarFile.getInputStream(entry)) {
				return extractType(in, type);
			}
		} catch (IOException _) {
			return Optional.empty();
		}
	}

	private <T extends TypeDecl> Optional<T> extractType(InputStream in, Class<T> type) {
		if (in == null) {
			return Optional.empty();
		}

		try {
			ExtractorSink sink = new ExtractorSink(1);
			extractor.processEntry(in.readAllBytes(), sink);

			if (sink.getTypes().size() != 1) {
				return Optional.empty();
			}

			TypeDecl foundType = sink.getTypes().iterator().next();
			return type.isInstance(foundType) ? Optional.of(type.cast(foundType)) : Optional.empty();
		} catch (IOException e) {
			return Optional.empty();
		}
	}
}
