package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StandardLibraryTypeProvider implements TypeProvider {
	private final AsmTypesExtractor extractor;

	private static final FileSystem JRT_FILESYSTEM = FileSystems.getFileSystem(URI.create("jrt:/"));
	private static final String MODULES_PATH = "modules";
	private static final Map<String, Module> PACKAGE_TO_MODULES =
		ModuleLayer.boot()
			.modules()
			.stream()
			.flatMap(m -> m.getPackages().stream().map(p -> Map.entry(p, m)))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	private static final Logger LOGGER = LogManager.getLogger(StandardLibraryTypeProvider.class);

	public StandardLibraryTypeProvider(AsmTypesExtractor extractor) {
		this.extractor = Preconditions.checkNotNull(extractor);
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		Preconditions.checkNotNull(qualifiedName);
		Preconditions.checkNotNull(type);

		String entry = nameToEntry(qualifiedName);
		String pkgName = packageName(qualifiedName);
		Module module = PACKAGE_TO_MODULES.get(pkgName);

		if (module != null) {
			Path clsFile = JRT_FILESYSTEM.getPath(MODULES_PATH, module.getName(), entry);

			if (Files.isRegularFile(clsFile)) {
				try {
					ExtractorSink sink = new ExtractorSink(1);
					byte[] clsBytes = Files.readAllBytes(clsFile);
					extractor.processEntry(clsBytes, sink);

					if (sink.getTypes().size() == 1) {
						TypeDecl foundType = sink.getTypes().iterator().next();
						if (type.isInstance(foundType)) {
							return Optional.of(type.cast(foundType));
						}
					}
				} catch (IOException e) {
					LOGGER.warn("Failed to process class file {}", clsFile, e);
				}
			}
		}

		return Optional.empty();
	}

	private static String packageName(String qualifiedName) {
		return qualifiedName.contains(".")
			? qualifiedName.substring(0, qualifiedName.lastIndexOf('.'))
			: "";
	}

	private static String nameToEntry(String name) {
		return name.replace('.', '/') + ".class";
	}
}
