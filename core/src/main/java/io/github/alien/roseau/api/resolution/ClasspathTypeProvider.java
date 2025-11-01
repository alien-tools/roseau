package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
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

public class ClasspathTypeProvider implements TypeProvider {
	private final AsmTypesExtractor extractor;
	private final Map<String, Path> entries = new HashMap<>();

	private static final Pattern ANONYMOUS_MATCHER = Pattern.compile("\\$\\d+");

	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath) {
		Preconditions.checkNotNull(extractor);
		Preconditions.checkNotNull(classpath);
		this.extractor = extractor;
		classpath.forEach(cp -> {
			try (JarFile jar = new JarFile(cp.toFile(), false, ZipFile.OPEN_READ, Runtime.version())) {
				jar.versionedStream()
					.filter(this::isRegularClassFile)
					.forEach(entry -> entries.put(entry.getName(), cp)); // classpath clash, priorities, etc.
			} catch (IOException e) {
				throw new RoseauException("Failed to process JAR file", e);
			}
		});
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		var found = entries.get(nameToEntry(qualifiedName));

		if (found != null) {
			ExtractorSink sink = new ExtractorSink(1);

			try (JarFile jar = new JarFile(found.toFile(), false, ZipFile.OPEN_READ, Runtime.version())) {
				var entry = jar.getJarEntry(nameToEntry(qualifiedName));

				if (entry != null) {
					extractor.processEntry(jar, entry, sink);
				}
			} catch (IOException e) {
				e.printStackTrace();
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
}
