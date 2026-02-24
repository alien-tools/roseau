package io.github.alien.roseau.footprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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
		Path v1Root = datasetRoot.resolve("v1").resolve("src").resolve("testing_lib");
		Path v2Root = datasetRoot.resolve("v2").resolve("src").resolve("testing_lib");

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
			List<Path> cases;
			try (Stream<Path> stream = Files.list(clientRoot)) {
				cases = stream.filter(Files::isDirectory).sorted().toList();
			}

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
				service.generateToFile(sourceTree, output, caseName, "Main");
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
		Path direct = cwd.resolve("jezek-dataset");
		if (Files.isDirectory(direct)) {
			return direct;
		}

		Path parent = cwd.resolve("..").resolve("jezek-dataset").normalize();
		if (Files.isDirectory(parent)) {
			return parent;
		}

		throw new IllegalArgumentException("Cannot locate jezek-dataset from " + cwd);
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
