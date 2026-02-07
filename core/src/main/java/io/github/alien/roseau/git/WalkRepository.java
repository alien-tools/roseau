package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WalkRepository {
	private final static String HEADER = "commit|date|message|" +
		"typesCount|methodsCount|fieldsCount|deprecatedAnnotationsCount|betaAnnotationsCount|" +
		"checkoutTime|apiTime|diffTime|statsTime|" +
		"breakingChangesCount|breakingChanges\n";

	static void main() throws Exception {
		new WalkRepository().walk(
			Path.of("/home/dig/repositories/guava/.git"),
			Path.of("/home/dig/repositories/guava/guava"),
			Path.of("/home/dig/repositories/guava/guava/pom.xml"),
			Path.of("guava.csv")
		);
	}

	void walk(Path gitDir, Path sources, Path pom, Path csv) throws Exception {
		Files.writeString(csv, HEADER,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		Stopwatch sw = Stopwatch.createUnstarted();
		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		try (Repository repo = builder.build(); Git git = new Git(repo); RevWalk rw = new RevWalk(repo)) {
			ObjectId headId = repo.resolve("HEAD");
			if (headId == null) {
				throw new IllegalStateException("Cannot resolve HEAD");
			}

			// Build linear history by following first parent only.
			List<RevCommit> chain = new ArrayList<>();
			RevCommit cur = rw.parseCommit(headId);
			while (true) {
				chain.add(cur);
				if (cur.getParentCount() == 0) break;
				cur = rw.parseCommit(cur.getParent(0)); // first parent only
			}
			Collections.reverse(chain); // oldest -> newest
			System.out.println(String.format("Walking %d commits", chain.size()));

			API oldApi = null;
			for (RevCommit commit : chain) {
				sw.reset().start();
				String sha = commit.getName();
				Date date = Date.from(Instant.ofEpochSecond(commit.getCommitTime()));
				String msg = commit.getShortMessage();
				git.checkout()
					.setName(sha)
					.setForced(true)
					.call();
				long checkoutTime = sw.elapsed().toMillis();

				System.out.println(String.format("Commit %s on %s: %s",
					sha, date, msg));

				if (!Files.exists(sources) || !Files.exists(pom)) {
					System.out.println("Skipping.");
					continue;
				}

				sw.reset().start();
				API currentApi = buildApi(sources, pom);
				long apiTime = sw.elapsed().toMillis();

				if (oldApi != null) {
					sw.reset().start();
					RoseauReport diff = Roseau.diff(oldApi, currentApi);
					long diffTime = sw.elapsed().toMillis();

					List<BreakingChange> bcs = diff.getBreakingChanges();
					System.out.println(String.format("Found %d breaking changes", bcs.size()));

					sw.reset().start();
					var numTypes = currentApi.getExportedTypes().size();
					var numMethods = currentApi.getExportedTypes().stream()
						.mapToInt(type -> type.getDeclaredMethods().size())
						.sum();
					var numFields = currentApi.getExportedTypes().stream()
						.mapToInt(type -> type.getDeclaredFields().size())
						.sum();
					var numDeprecatedAnnotations = getApiAnnotationsCount(currentApi, "java.lang.Deprecated");
					var numBetaAnnotations = getApiAnnotationsCount(currentApi, "com.google.common.annotations.Beta");
					long statsTime = sw.elapsed().toMillis();

					var line = "%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(
						sha, date, msg.replace("|", " "),
						numTypes, numMethods, numFields, numDeprecatedAnnotations, numBetaAnnotations,
						checkoutTime, apiTime, diffTime, statsTime,
						bcs.size(), bcs.stream().map(BreakingChange::toString).collect(Collectors.joining(",")));

					Files.writeString(csv, line, StandardOpenOption.APPEND);
				}

				oldApi = currentApi;
			}
		}
	}

	API buildApi(Path sources, Path pom) {
		Library library = Library.builder()
			.location(sources)
			.pom(pom)
			.build();
		TypesExtractor extractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
		LibraryTypes types = extractor.extractTypes(library);
		return types.toAPI();
	}

	long getApiAnnotationsCount(API api, String fqn) {
		return api.getExportedTypes().stream()
			.mapToLong(type -> {
				var typeAnnotationCount = type.getAnnotations().stream()
					.filter(a -> a.actualAnnotation().getQualifiedName().equals(fqn))
					.count();
				var fieldsAnnotationCount = type.getDeclaredFields().stream()
					.filter(f -> f.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();
				var methodsAnnotationCount = type.getDeclaredMethods().stream()
					.filter(m -> m.getAnnotations().stream().anyMatch(a -> a.actualAnnotation().getQualifiedName().equals(fqn)))
					.count();

				return typeAnnotationCount + fieldsAnnotationCount + methodsAnnotationCount;
			})
			.sum();
	}
}
