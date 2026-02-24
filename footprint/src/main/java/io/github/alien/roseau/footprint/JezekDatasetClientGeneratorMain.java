package io.github.alien.roseau.footprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Regenerates client sources under {@code jezek-dataset/client/src}.
 */
public final class JezekDatasetClientGeneratorMain {
	private JezekDatasetClientGeneratorMain() {

	}

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.err.println("Usage: JezekDatasetClientGeneratorMain [dataset-root]");
			return;
		}

		Path datasetRoot = args.length == 1
			? Path.of(args[0]).toAbsolutePath().normalize()
			: locateDatasetRoot(Path.of("").toAbsolutePath().normalize());

		Path clientRoot = datasetRoot.resolve("client").resolve("src");
		Path v1Root = resolveVersionRoot(datasetRoot.resolve("v1").resolve("src"));
		Path v2Root = resolveVersionRoot(datasetRoot.resolve("v2").resolve("src"));

		if (!Files.isDirectory(clientRoot)) {
			throw new IllegalArgumentException("Missing client root: " + clientRoot);
		}
		if (!Files.isDirectory(v1Root)) {
			throw new IllegalArgumentException("Missing v1 source root: " + v1Root);
		}
		if (!Files.isDirectory(v2Root)) {
			throw new IllegalArgumentException("Missing v2 source root: " + v2Root);
		}

		FootprintService service = new FootprintService();
		Path emptySourceRoot = Files.createTempDirectory("roseau-jezek-empty");

		int generated = 0;
		int emptyFallbacks = 0;
		try {
			List<Path> cases = findCaseDirectories(clientRoot, v1Root, v2Root);

			for (Path caseDir : cases) {
				String caseName = caseDir.getFileName().toString();
				Path v1Case = v1Root.resolve(caseName);
				Path v2Case = v2Root.resolve(caseName);

				Path sourceTree;
				if (Files.isDirectory(v1Case)) {
					sourceTree = v1Case;
				} else if (Files.isDirectory(v2Case)) {
					sourceTree = emptySourceRoot;
					emptyFallbacks++;
				} else {
					throw new IllegalStateException("Case not found in v1 or v2: " + caseName);
				}

				Path output = caseDir.resolve("Main.java");
				String packageName = toPackageName(clientRoot.relativize(caseDir));
				service.generateToFile(sourceTree, output, packageName, "Main");
				generated++;
			}
		} finally {
			deleteRecursively(emptySourceRoot);
		}

		System.out.println("Generated " + generated + " client files under " + clientRoot);
		if (emptyFallbacks > 0) {
			System.out.println("Used empty-v1 fallback for " + emptyFallbacks + " v2-only case(s)");
		}
	}

	private static Path locateDatasetRoot(Path cwd) {
		Path normalizedCwd = cwd.toAbsolutePath().normalize();
		List<Path> candidates = new ArrayList<>();
		candidates.add(normalizedCwd);
		candidates.add(normalizedCwd.resolve("..").normalize());

		for (Path candidate : candidates) {
			if (hasDatasetLayout(candidate)) {
				return candidate;
			}
			try (Stream<Path> stream = Files.list(candidate)) {
				for (Path child : stream.filter(Files::isDirectory).toList()) {
					if (hasDatasetLayout(child)) {
						return child;
					}
				}
			} catch (IOException _) {
				// Ignore unreadable candidates and continue searching.
			}
		}

		throw new IllegalArgumentException("Cannot locate dataset root with expected layout from " + cwd);
	}

	private static boolean hasDatasetLayout(Path root) {
		return Files.isDirectory(root.resolve("client").resolve("src")) &&
			Files.isDirectory(root.resolve("v1").resolve("src")) &&
			Files.isDirectory(root.resolve("v2").resolve("src"));
	}

	private static List<Path> findCaseDirectories(Path clientRoot, Path v1Root, Path v2Root) throws IOException {
		try (Stream<Path> stream = Files.walk(clientRoot)) {
			return stream
				.filter(Files::isDirectory)
				.filter(path -> !path.equals(clientRoot))
				.filter(path -> {
					String caseName = path.getFileName().toString();
					return Files.isDirectory(v1Root.resolve(caseName)) || Files.isDirectory(v2Root.resolve(caseName));
				})
				.sorted()
				.toList();
		}
	}

	private static String toPackageName(Path relativePath) {
		return StreamSupport.stream(relativePath.spliterator(), false)
			.map(Path::toString)
			.collect(Collectors.joining("."));
	}

	private static Path resolveVersionRoot(Path srcRoot) {
		Path testingLib = srcRoot.resolve("testing_lib");
		return Files.isDirectory(testingLib) ? testingLib : srcRoot;
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || Files.notExists(root)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(root)) {
			for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}
}
