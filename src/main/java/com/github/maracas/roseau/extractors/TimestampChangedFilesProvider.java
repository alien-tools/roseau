package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.API;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimestampChangedFilesProvider implements ChangedFilesProvider {
	private final Path sources;
	private API previousApi;
	private long timestamp;

	public TimestampChangedFilesProvider(Path sources) {
		this.sources = sources;
	}

	public void refresh(API previousApi, long timestamp) {
		this.previousApi = previousApi;
		this.timestamp = timestamp;
	}

	@Override
	public ChangedFiles getChangedFiles() {
		try (Stream<Path> files = Files.walk(sources)) {
			Set<Path> currentFiles = files
				.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java") &&
					!p.endsWith("package-info.java") && !p.endsWith("module-info.java"))
				.map(Path::toAbsolutePath)
				.collect(Collectors.toSet());

			if (previousApi == null) {
				return new ChangedFiles(Set.of(), Set.of(), currentFiles);
			}

			Set<Path> previousFiles = previousApi.getAllTypes()
				.map(d -> d.getLocation().file().toAbsolutePath())
				.collect(Collectors.toSet());

			Set<Path> removedFiles = Sets.difference(previousFiles, currentFiles);
			Set<Path> createdFiles = Sets.difference(currentFiles, previousFiles);
			Set<Path> updatedFiles = currentFiles.stream()
				.filter(f -> previousFiles.contains(f) && f.toFile().lastModified() > timestamp)
				.collect(Collectors.toSet());

			return new ChangedFiles(updatedFiles, removedFiles, createdFiles);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Path getSources() {
		return sources;
	}
}
