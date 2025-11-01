package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class StandardLibraryTypeProvider implements TypeProvider {
	private final AsmTypesExtractor extractor;
	private final List<Path> jmods;

	private static final String CLASSES_PREFIX = "classes";

	public StandardLibraryTypeProvider(AsmTypesExtractor extractor, List<String> jmodNames) {
		Preconditions.checkNotNull(extractor);
		Preconditions.checkNotNull(jmodNames);
		this.extractor = extractor;
		Path jmodsPath = Path.of(System.getProperty("java.home"), "jmods");
		if (!Files.isDirectory(jmodsPath)) {
			throw new RoseauException("Couldn't resolve jmods path: " + jmodsPath);
		}
		jmods = jmodNames.stream()
			.<Path>mapMulti((jmod, downstream) -> {
				var jmodPath = jmodsPath.resolve(jmod + ".jmod");
				if (Files.isRegularFile(jmodPath)) {
					downstream.accept(jmodPath);
				} else {
					throw new RoseauException("Couldn't resolve jmod file: " + jmodPath);
				}
			})
			.toList();
	}

	public StandardLibraryTypeProvider(AsmTypesExtractor extractor) {
		this(extractor, List.of("java.base"));
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		var sink = new ExtractorSink(1);

		for (var jmod : jmods) {
			try (JarFile jar = new JarFile(jmod.toFile(), false, ZipFile.OPEN_READ, Runtime.version())) {
				var entryName = CLASSES_PREFIX + "/" + nameToEntry(qualifiedName);
				var entry = jar.getJarEntry(entryName); // FIXME: Windows?
				if (entry != null) {
					extractor.processEntry(jar, entry, sink);
					if (sink.getTypes().size() == 1) {
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (sink.getTypes().size() == 1) {
			return Optional.of(type.cast(sink.getTypes().iterator().next()));
		} else {
			return Optional.empty();
		}
	}

	private String nameToEntry(String name) {
		return name.replace('.', '/') + ".class";
	}
}
