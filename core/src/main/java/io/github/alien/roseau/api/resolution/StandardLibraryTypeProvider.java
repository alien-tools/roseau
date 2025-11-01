package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class StandardLibraryTypeProvider implements TypeProvider, AutoCloseable {
	private final AsmTypesExtractor extractor;
	private final Map<String, Path> entryToJmod = new HashMap<>();
	private final LoadingCache<Path, JarFile> jarCache = CacheBuilder.newBuilder()
		.maximumSize(20)
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

	private static final String CLASSES_PREFIX = "classes";

	public StandardLibraryTypeProvider(AsmTypesExtractor extractor, List<String> jmodNames) {
		Preconditions.checkNotNull(extractor);
		Preconditions.checkNotNull(jmodNames);
		this.extractor = extractor;

		// Validate and resolve jmod paths
		Path jmodsPath = Path.of(System.getProperty("java.home"), "jmods");
		if (!Files.isDirectory(jmodsPath)) {
			throw new RoseauException("Couldn't resolve jmods path: " + jmodsPath);
		}

		List<Path> jmodPaths = jmodNames.stream()
			.<Path>mapMulti((jmod, downstream) -> {
				var jmodPath = jmodsPath.resolve(jmod + ".jmod");
				if (Files.isRegularFile(jmodPath)) {
					downstream.accept(jmodPath);
				} else {
					throw new RoseauException("Couldn't resolve jmod file: " + jmodPath);
				}
			})
			.toList();

		// Build index: entry name -> jmod path
		jmodPaths.forEach(jmodPath -> {
			try (JarFile jar = new JarFile(jmodPath.toFile(), false, ZipFile.OPEN_READ, Runtime.version())) {
				jar.stream()
					.filter(entry -> !entry.isDirectory()
						&& entry.getName().startsWith(CLASSES_PREFIX + "/")
						&& entry.getName().endsWith(".class"))
					.forEach(entry -> entryToJmod.putIfAbsent(entry.getName(), jmodPath));
			} catch (IOException e) {
				throw new RoseauException("Failed to index jmod file %s".formatted(jmodPath), e);
			}
		});
	}

	public StandardLibraryTypeProvider(AsmTypesExtractor extractor) {
		this(extractor, List.of("java.base"));
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		String entryName = CLASSES_PREFIX + "/" + nameToEntry(qualifiedName);
		Path jmodPath = entryToJmod.get(entryName);

		if (jmodPath != null) {
			ExtractorSink sink = new ExtractorSink(1);

			try {
				JarFile jar = jarCache.getUnchecked(jmodPath);
				var entry = jar.getJarEntry(entryName);

				if (entry != null) {
					extractor.processEntry(jar, entry, sink);
				}
			} catch (Exception e) {
				throw new RoseauException("Failed to process jmod file %s".formatted(jmodPath), e);
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

	@Override
	public void close() {
		// Invalidate all entries, triggering removal listener to close all JarFiles
		jarCache.invalidateAll();
	}
}
