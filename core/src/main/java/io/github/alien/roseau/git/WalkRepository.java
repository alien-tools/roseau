package io.github.alien.roseau.git;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.MavenClasspathBuilder;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
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
		"checkoutTime|classpathTime|apiTime|diffTime|statsTime|" +
		"breakingChangesCount|breakingChanges\n";
	private static final Logger LOGGER = LogManager.getLogger(WalkRepository.class);

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
			LOGGER.info("Walking {} commits", chain.size());

			MavenClasspathBuilder maven = new MavenClasspathBuilder();
			API oldApi = null;
			List<Path> classpath = List.of();
			for (RevCommit commit : chain) {
				sw.reset().start();
				String sha = commit.getName();
				Date date = Date.from(Instant.ofEpochSecond(commit.getCommitTime()));
				String msg = commit.getShortMessage();
				git.checkout()
					.setName(sha)
					.setForced(true)
					.call();
				Flags flags = changedJavaOrPom(repo, commit);
				long checkoutTime = sw.elapsed().toMillis();

				LOGGER.info("Commit {} on {}: {}", sha, date, msg);

				if (!Files.exists(sources) || !Files.exists(pom)) {
					LOGGER.info("Skipping.");
					continue;
				}

				long classpathTime = 0L;
				if (classpath.isEmpty() || flags.pomChanged()) {
					sw.reset().start();
					List<Path> cp = maven.buildClasspath(pom);
					if (!cp.isEmpty()) {
						classpath = cp;
					} else {
						LOGGER.warn("Couldn't build classpath");
					}
					classpathTime = sw.elapsed().toMillis();
					LOGGER.info("Recomputing classpath took {}ms: {}", classpathTime, classpath);
				}

				sw.reset().start();
				API currentApi = buildApi(sources, classpath);
				long apiTime = sw.elapsed().toMillis();

				if (oldApi != null) {
					sw.reset().start();
					RoseauReport diff = Roseau.diff(oldApi, currentApi);
					long diffTime = sw.elapsed().toMillis();

					List<BreakingChange> bcs = diff.getBreakingChanges();
					LOGGER.info("Found {} breaking changes", bcs.size());

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

					var line = "%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(
						sha, date, msg.replace("|", " "),
						numTypes, numMethods, numFields, numDeprecatedAnnotations, numBetaAnnotations,
						checkoutTime, classpathTime, apiTime, diffTime, statsTime,
						bcs.size(), bcs.stream().map(BreakingChange::toString).collect(Collectors.joining(",")));

					Files.writeString(csv, line, StandardOpenOption.APPEND);
				}

				oldApi = currentApi;
			}
		}
	}

	static API buildApi(Path sources, List<Path> classpath) {
		LOGGER.info("Classpath is {}", classpath);
		Library library = Library.builder()
			.location(sources)
			.classpath(classpath)
			.build();
		TypesExtractor extractor = new JdtTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()));
		LibraryTypes types = extractor.extractTypes(library);
		return types.toAPI();
	}

	static long getApiAnnotationsCount(API api, String fqn) {
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

	record Flags(boolean javaChanged, boolean pomChanged) {
	}

	static Flags changedJavaOrPom(Repository repo, RevCommit commit) throws IOException {
		try (RevWalk rw = new RevWalk(repo);
		     ObjectReader reader = repo.newObjectReader();
		     DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

			df.setRepository(repo);
			df.setDetectRenames(false);

			// New tree (this commit)
			CanonicalTreeParser newTree = new CanonicalTreeParser();
			newTree.reset(reader, commit.getTree().getId());

			// Old tree (first parent) or empty tree for initial commit
			CanonicalTreeParser oldTree = new CanonicalTreeParser();
			if (commit.getParentCount() > 0) {
				RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
				oldTree.reset(reader, parent.getTree().getId());
			} else {
				ObjectId emptyTreeId = repo.resolve(Constants.EMPTY_TREE_ID.name());
				oldTree.reset(reader, emptyTreeId);
			}

			List<DiffEntry> diffs = df.scan(oldTree, newTree);

			boolean javaChanged = false;
			boolean pomChanged = false;

			for (DiffEntry e : diffs) {
				// For deletions, newPath is DEV_NULL, so check both sides
				String path = e.getNewPath();
				if (DiffEntry.DEV_NULL.equals(path)) {
					path = e.getOldPath();
				}

				if (!javaChanged && path.endsWith(".java")) {
					javaChanged = true;
				}

				// Match root pom.xml and module poms
				if (!pomChanged && (path.equals("pom.xml") || path.endsWith("/pom.xml"))) {
					pomChanged = true;
				}

				if (javaChanged && pomChanged) {
					break;
				}
			}

			return new Flags(javaChanged, pomChanged);
		}
	}
}
