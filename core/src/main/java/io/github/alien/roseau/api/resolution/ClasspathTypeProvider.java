package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheLoader;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class ClasspathTypeProvider implements TypeProvider, AutoCloseable {
	private final AsmTypesExtractor extractor;
	private final Map<String, Path> entryToJar = new HashMap<>();
	private final LoadingCache<Path, JarFile> jarCache = CacheBuilder.newBuilder()
		.maximumSize(100)
		.removalListener(notification -> {
			JarFile jar = (JarFile) notification.getValue();
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException ignored) {
					// Don't throw on cleanup
				}
			}
		})
		.build(new CacheLoader<>() {
			@Override
			public JarFile load(Path path) throws IOException {
				return new JarFile(path.toFile(), false, ZipFile.OPEN_READ, Runtime.version());
			}
		});

	private static final Pattern ANONYMOUS_MATCHER = Pattern.compile("\\$\\d+");

	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath) {
		Preconditions.checkNotNull(extractor);
		Preconditions.checkNotNull(classpath);
		this.extractor = extractor;

		// Build index: entry name -> JAR path
		// Use putIfAbsent to ensure first JAR on classpath wins
		classpath.forEach(cp -> {
			try (JarFile jar = new JarFile(cp.toFile(), false, ZipFile.OPEN_READ, Runtime.version())) {
				jar.versionedStream()
					.filter(this::isRegularClassFile)
					.forEach(entry -> entryToJar.putIfAbsent(entry.getName(), cp));
			} catch (IOException e) {
				throw new RoseauException("Failed to process JAR file %s".formatted(cp), e);
			}
		});
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		String entryName = nameToEntry(qualifiedName);
		Path jarPath = entryToJar.get(entryName);

		if (jarPath != null) {
			ExtractorSink sink = new ExtractorSink(1);

			try {
				// Use cached JarFile - no need to close, managed by cache
				JarFile jar = jarCache.getUnchecked(jarPath);
				var entry = jar.getJarEntry(entryName);

				if (entry != null) {
					extractor.processEntry(jar, entry, sink);
				}
			} catch (Exception e) {
				throw new RoseauException("Failed to process JAR file %s".formatted(jarPath), e);
			}

			if (sink.getTypes().size() == 1) {
				return Optional.of(type.cast(sink.getTypes().iterator().next()));
			}
		}

		return Optional.empty();
	}

	private String nameToEntry(String name) {
		return name.replace('.', '/') + ".class";
	}

	private boolean isRegularClassFile(JarEntry entry) {
		return !entry.isDirectory()
			&& entry.getName().endsWith(".class")
			&& !ANONYMOUS_MATCHER.matcher(entry.getName()).find();
	}

	@Override
	public void close() {
		jarCache.invalidateAll();
	}
}
