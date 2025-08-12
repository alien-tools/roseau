package io.github.alien.roseau.extractors.incremental;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.RoseauException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple provider of {@link ChangedFiles} that uses timestamp-based modification times of files to infer files that
 * have been created, updated, and deleted between two instants.
 */
public class TimestampChangedFilesProvider implements ChangedFilesProvider {
	private final Path sources;
	private final Set<Path> previousFiles;
	private final long timestamp;

	/**
	 * Creates a new provider for a given source, set of previous files, and a timestamp
	 *
	 * @throws NullPointerException     if sources or previousFiles is null
	 * @throws IllegalArgumentException if {@code sources} does not exist
	 */
	public TimestampChangedFilesProvider(Path sources, Set<Path> previousFiles, long timestamp) {
		this.sources = Objects.requireNonNull(sources);
		Preconditions.checkArgument(Files.exists(sources), "Directory not found:" + sources);
		this.previousFiles = Objects.requireNonNull(previousFiles);
		this.timestamp = timestamp;
	}

	public TimestampChangedFilesProvider(Path sources, Set<Path> previousFiles) {
		this(sources, previousFiles, Instant.now().toEpochMilli());
	}

	@Override
	public ChangedFiles getChangedFiles() {
		try (Stream<Path> files = Files.walk(sources)) {
			Set<Path> currentFiles = files
				.filter(TimestampChangedFilesProvider::isRegularJavaFile)
				.map(Path::toAbsolutePath)
				.collect(Collectors.toSet());

			if (previousFiles.isEmpty()) {
				return new ChangedFiles(Set.of(), Set.of(), currentFiles);
			}

			Set<Path> deletedFiles = Sets.difference(previousFiles, currentFiles);
			Set<Path> createdFiles = Sets.difference(currentFiles, previousFiles);
			Set<Path> updatedFiles = currentFiles.stream()
				.filter(f -> previousFiles.contains(f) && f.toFile().lastModified() > timestamp)
				.collect(Collectors.toSet());

			return new ChangedFiles(updatedFiles, deletedFiles, createdFiles);
		} catch (IOException e) {
			throw new RoseauException("Failed to read changed files from " + sources, e);
		}
	}

	private static boolean isRegularJavaFile(Path file) {
		return Files.isRegularFile(file) &&
			file.toString().endsWith(".java") &&
			!file.endsWith("package-info.java") &&
			!file.endsWith("module-info.java");
	}
}
