package io.github.alien.roseau.api.resolution;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.extractors.ExtractorSink;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ClasspathTypeProvider implements TypeProvider, AutoCloseable {
	private final AsmTypesExtractor extractor;
	private final URLClassLoader classLoader;

	public ClasspathTypeProvider(AsmTypesExtractor extractor, List<Path> classpath) {
		Preconditions.checkNotNull(extractor);
		Preconditions.checkNotNull(classpath);

		URL[] jarUrls = classpath.stream()
			.<URL>mapMulti((jar, downstream) -> {
				try {
					downstream.accept(jar.toUri().toURL());
				} catch (MalformedURLException _) {
					// Just ignore
				}
			})
			.toArray(URL[]::new);

		// No ClassLoader.getSystemClassLoader() cause we don't want Roseau's classpath to interfere
		this.classLoader = new URLClassLoader(jarUrls, ClassLoader.getPlatformClassLoader());
		this.extractor = extractor;
	}

	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		String entryName = nameToEntry(qualifiedName);
		try (InputStream in = classLoader.getResourceAsStream(entryName)) {
			if (in != null) {
				ExtractorSink sink = new ExtractorSink(1);
				byte[] bytes = in.readAllBytes();
				extractor.processEntry(bytes, sink);

				if (sink.getTypes().size() == 1) {
					TypeDecl foundType = sink.getTypes().iterator().next();
					if (type.isInstance(foundType)) {
						return Optional.of(type.cast(foundType));
					}
				}
			}
		} catch (IOException _) {
			// Just ignore
		}

		return Optional.empty();
	}

	private String nameToEntry(String name) {
		return name.replace('.', '/') + ".class";
	}

	@Override
	public void close() throws IOException {
		classLoader.close();
	}
}
