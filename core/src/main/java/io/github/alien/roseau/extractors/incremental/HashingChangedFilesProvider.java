package io.github.alien.roseau.extractors.incremental;

import io.github.alien.roseau.RoseauException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * A provider that identifies changes (updates, deletions, and creations) between two directories containing .java
 * files. The comparison is performed using hash values computed via the supplied {@link HashFunction}.
 */
public class HashingChangedFilesProvider {
	private final HashFunction hashFunction;

	public HashingChangedFilesProvider(HashFunction hashFunction) {
		this.hashFunction = hashFunction;
	}

	/**
	 * Compares two directories and identifies .java files that have been updated, deleted, or created, using the first
	 * directory as reference. The returned files use relative paths.
	 *
	 * @param leftDirectory  the first (reference) directory
	 * @param rightDirectory the second (new) directory
	 * @return a {@link ChangedFiles} specifying the updated, deleted, and created files
	 * @throws RoseauException if an error occurs during file scanning or hash calculation
	 */
	public ChangedFiles getChangedFiles(Path leftDirectory, Path rightDirectory) {
		try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
			Set<Path> deleted = new HashSet<>();
			Set<Path> updated = new HashSet<>();

			Future<Map<Path, Long>> leftFuture = virtualExecutor.submit(() -> scanJavaFiles(leftDirectory, virtualExecutor));
			Future<Map<Path, Long>> rightFuture = virtualExecutor.submit(() -> scanJavaFiles(rightDirectory, virtualExecutor));
			Map<Path, Long> leftHashes = leftFuture.get();
			Map<Path, Long> rightHashes = rightFuture.get();

			leftHashes.forEach((file, leftHash) -> {
				Long rightHash = rightHashes.remove(file);
				if (rightHash == null) {
					deleted.add(file);
				} else if (!leftHash.equals(rightHash)) {
					updated.add(file);
				}
			});

			return new ChangedFiles(updated, deleted, rightHashes.keySet());
		} catch (InterruptedException | ExecutionException e) {
			throw new RoseauException("Couldn't compute changed files", e);
		}
	}

	Map<Path, Long> scanJavaFiles(Path root, ExecutorService executor) throws IOException {
		Map<Path, Future<Long>> futures = new ConcurrentHashMap<>();

		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				Path relative = root.relativize(file);
				if (relative.toString().endsWith(".java")) {
					futures.put(relative, executor.submit(() -> hashFunction.hash(file)));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return futures.entrySet().stream().collect(Collectors.toMap(
			Map.Entry::getKey,
			entry -> {
				try {
					return entry.getValue().get();
				} catch (InterruptedException | ExecutionException e) {
					return -1L;
				}
			}
		));
	}
}
