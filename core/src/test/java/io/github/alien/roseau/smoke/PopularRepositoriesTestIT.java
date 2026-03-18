package io.github.alien.roseau.smoke;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.git.CommitAnalysis;
import io.github.alien.roseau.git.GitWalker;
import io.github.alien.roseau.options.RoseauOptions;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that clones popular library repositories and walks their commit history
 * using {@link GitWalker} with incremental analysis. Verifies that the incremental
 * pipeline produces correct results and reports performance metrics.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PopularRepositoriesTestIT {
	private static final Path CLONE_CACHE_DIR = Path.of("target", "popular-repos-clones");
	private static final String REPOSITORIES_RESOURCE = "/popular-repositories.tsv";
	private static final RoseauOptions.Exclude NO_EXCLUSIONS =
		new RoseauOptions.Exclude(List.of(), List.of());
	private static final int SETUP_CONCURRENCY = Math.max(1, Runtime.getRuntime().availableProcessors());

	record RepoSpec(boolean enabled, String libraryId, String url, List<String> sourceRoots, String reason) {
	}

	static Map<String, GitWalker.Config> prepared = new ConcurrentHashMap<>();

	static Stream<String> repositories() {
		return loadRepoSpecs().stream()
			.filter(RepoSpec::enabled)
			.map(RepoSpec::libraryId);
	}

	private static List<RepoSpec> loadRepoSpecs() {
		try (InputStream is = PopularRepositoriesTestIT.class.getResourceAsStream(REPOSITORIES_RESOURCE)) {
			if (is == null) {
				throw new IllegalStateException("Missing resource " + REPOSITORIES_RESOURCE);
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				return reader.lines()
					.map(String::strip)
					.filter(line -> !line.isEmpty())
					.filter(line -> !line.startsWith("#"))
					.map(PopularRepositoriesTestIT::parseRepoSpec)
					.toList();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static RepoSpec parseRepoSpec(String line) {
		var parts = line.split("\t");
		if (parts.length < 4) {
			throw new IllegalArgumentException("Invalid repository entry (need enabled, id, url, sourceRoots): " + line);
		}
		var enabled = switch (parts[0]) {
			case "true" -> true;
			case "false" -> false;
			default -> throw new IllegalArgumentException("Invalid enabled flag in entry: " + line);
		};
		List<String> sourceRoots = Arrays.stream(parts[3].split(";"))
			.map(String::strip)
			.filter(s -> !s.isEmpty())
			.toList();
		var reason = parts.length >= 5 ? parts[4] : "";
		return new RepoSpec(enabled, parts[1], parts[2], sourceRoots, reason);
	}

	@BeforeAll
	void setUp() {
		var specs = loadRepoSpecs().stream()
			.filter(RepoSpec::enabled)
			.toList();
		var total = specs.size();

		System.out.printf("Preparing %d repository clones using %d concurrent slots%n", total, SETUP_CONCURRENCY);
		try (var executor = Executors.newFixedThreadPool(SETUP_CONCURRENCY)) {
			var futures = IntStream.range(0, total)
				.mapToObj(index -> executor.submit(() -> {
					var spec = specs.get(index);
					var sw = Stopwatch.createStarted();
					System.out.printf("[setup %d/%d] Cloning %s%n", index + 1, total, spec.libraryId());

					Path cloneDir = CLONE_CACHE_DIR.resolve(spec.libraryId());
					Path gitDir = cloneDir.resolve(".git");
					List<Path> sourceRoots = spec.sourceRoots().stream()
						.map(cloneDir::resolve)
						.toList();

					GitWalker.prepareRepository(spec.url(), gitDir);

					var config = new GitWalker.Config(
						spec.libraryId(), spec.url(), gitDir, sourceRoots, NO_EXCLUSIONS);

					System.out.printf("[setup %d/%d] Ready %s in %dms%n",
						index + 1, total, spec.libraryId(), sw.elapsed().toMillis());
					return Map.entry(spec.libraryId(), config);
				}))
				.toList();

			for (var future : futures) {
				var entry = future.get();
				prepared.put(entry.getKey(), entry.getValue());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("repositories")
	@Timeout(value = 15, unit = TimeUnit.MINUTES)
	void walkRepository(String libraryId) throws Exception {
		var config = prepared.get(libraryId);

		List<CommitAnalysis> analyses = new ArrayList<>();
		var sw = Stopwatch.createStarted();
		new GitWalker(config).walk(analyses::add);
		long totalTime = sw.elapsed().toMillis();

		// Compute statistics
		long commitsWithApi = analyses.stream().filter(a -> a.api().isPresent()).count();
		long commitsWithJavaChanges = analyses.stream().filter(a -> a.commit().javaChanged()).count();
		long commitsWithErrors = analyses.stream().filter(a -> !a.errors().isEmpty()).count();
		long commitsWithApiChanges = analyses.stream().filter(CommitAnalysis::apiChanged).count();
		long commitsWithBCs = analyses.stream()
			.filter(a -> a.report().isPresent())
			.filter(a -> !a.report().get().getAllBreakingChanges().isEmpty())
			.count();
		long totalBCs = analyses.stream()
			.flatMap(a -> a.report().stream())
			.mapToInt(r -> r.getAllBreakingChanges().size())
			.sum();
		long totalApiTimeMs = analyses.stream().mapToLong(CommitAnalysis::apiTimeMs).sum();
		long totalDiffTimeMs = analyses.stream().mapToLong(CommitAnalysis::diffTimeMs).sum();
		long totalCheckoutTimeMs = analyses.stream().mapToLong(CommitAnalysis::checkoutTimeMs).sum();
		long maxApiTime = analyses.stream().mapToLong(CommitAnalysis::apiTimeMs).max().orElse(0);

		var lastApi = analyses.reversed().stream()
			.flatMap(a -> a.api().stream())
			.findFirst()
			.orElse(null);
		int finalTypeCount = lastApi != null ? lastApi.getLibraryTypes().getAllTypes().size() : 0;
		int finalExportedTypeCount = lastApi != null ? lastApi.getExportedTypes().size() : 0;

		System.out.printf(
			"Walked %s: %d commits in %dms%n" +
				"\tJava changes: %d, API changes: %d, With BCs: %d%n" +
				"\tTotal BCs: %d, Incremental errors: %d%n" +
				"\tFinal API: %d types (%d exported)%n" +
				"\tTiming: checkout=%dms, api=%dms (max=%dms), diff=%dms%n",
			libraryId, analyses.size(), totalTime,
			commitsWithJavaChanges, commitsWithApiChanges, commitsWithBCs,
			totalBCs, commitsWithErrors,
			finalTypeCount, finalExportedTypeCount,
			totalCheckoutTimeMs, totalApiTimeMs, maxApiTime, totalDiffTimeMs);

		if (commitsWithErrors > 0) {
			analyses.stream()
				.filter(a -> !a.errors().isEmpty())
				.forEach(a -> System.out.printf("\tError at %s: %s%n", a.commit().sha(),
					a.errors().stream().map(Exception::getMessage).collect(Collectors.joining("; "))));
		}

		// We processed commits
		assertThat(analyses)
			.as("Walk should process commits")
			.isNotEmpty();

		// At least some commits produced APIs
		assertThat(commitsWithApi)
			.as("At least one commit should produce an API")
			.isGreaterThan(0);

		// Incremental analysis never fell back to full rebuild
		assertThat(commitsWithErrors)
			.as("Incremental analysis should not fall back to full rebuild")
			.isZero();

		// The final API has types
		assertThat(finalTypeCount)
			.as("Final API should contain types")
			.isGreaterThan(0);

		// Verify incremental accuracy: compare the last incremental API with a full rebuild at HEAD
		var spec = loadRepoSpecs().stream()
			.filter(s -> s.libraryId().equals(libraryId))
			.findFirst()
			.orElseThrow();
		Path cloneDir = CLONE_CACHE_DIR.resolve(libraryId);
		Path activeSourceRoot = spec.sourceRoots().stream()
			.map(cloneDir::resolve)
			.filter(Files::isDirectory)
			.findFirst()
			.orElse(null);

		if (activeSourceRoot != null && lastApi != null) {
			sw.reset().start();
			var fullApi = Roseau.buildAPI(Library.builder()
				.location(activeSourceRoot)
				.classpath(List.of())
				.build());
			long fullBuildTime = sw.elapsed().toMillis();

			System.out.printf("\tFull rebuild verification: %dms (%d types, %d exported)%n",
				fullBuildTime,
				fullApi.getLibraryTypes().getAllTypes().size(),
				fullApi.getExportedTypes().size());

			assertThat(lastApi.getLibraryTypes())
				.as("Incremental types at HEAD should match full rebuild")
				.isEqualTo(fullApi.getLibraryTypes();

			assertThat(lastApi)
				.as("Incremental API at HEAD should match full rebuild")
				.isEqualTo(fullApi);
		} else {
			Assertions.fail("Couldn't compare final APIs");
		}
	}
}
