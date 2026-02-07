package io.github.alien.roseau.git;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
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

/**
 * Helpful resources when setting up a repository for analysis:
 * - git log --reverse --format="%H %cd %s" -- src/main/java | head -n 1
 * - git log --reverse --format="%H %cd %s" -- pom.xml | head -n 1
 */
public class WalkRepository {
	private final static String HEADER = "commit|date|message|commit_url|" +
		"typesCount|methodsCount|fieldsCount|deprecatedAnnotationsCount|betaAnnotationsCount|" +
		"checkoutTime|classpathTime|apiTime|diffTime|statsTime|" +
		"breakingChangesCount|breakingChanges\n";
	private static final Logger LOGGER = LogManager.getLogger(WalkRepository.class);

	public record Repository(
		String id,
		String url,
		Path gitDir,
		List<Path> sourceRoots,
		//List<Path> poms,
		Path csv
	) {
	}

	private static final ObjectMapper MAPPER = createMapper();

	private static ObjectMapper createMapper() {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());

		SimpleModule pathModule = new SimpleModule();
		pathModule.addDeserializer(Path.class,
			new com.fasterxml.jackson.databind.JsonDeserializer<>() {
				@Override
				public Path deserialize(com.fasterxml.jackson.core.JsonParser p,
				                        com.fasterxml.jackson.databind.DeserializationContext ctxt)
					throws IOException {
					return Path.of(p.getValueAsString());
				}
			});
		pathModule.addSerializer(Path.class,
			new com.fasterxml.jackson.databind.JsonSerializer<>() {
				@Override
				public void serialize(Path value,
				                      com.fasterxml.jackson.core.JsonGenerator gen,
				                      com.fasterxml.jackson.databind.SerializerProvider serializers)
					throws IOException {
					gen.writeString(value.toString());
				}
			});

		om.registerModule(pathModule);
		return om;
	}

	public static List<Repository> loadConfig(Path yamlFile) throws IOException {
		return MAPPER.readValue(
			yamlFile.toFile(),
			new TypeReference<List<Repository>>() {
			}
		);
	}

	static void main() throws Exception {
		Path config = Path.of("walk.yaml");

		List<Repository> repos = loadConfig(config);

		repos.stream().forEach(repo -> {
			try {
				walk(repo.url(), repo.gitDir(), repo.sourceRoots(), List.of(), repo.csv());
			} catch (Exception e) {
				LOGGER.error("Analysis of {} failed: {}", repo.url());
				e.printStackTrace();
			}
		});
	}

	static void walk(String url, Path gitDir, List<Path> sources, List<Path> poms, Path csv) throws Exception {
		Files.writeString(csv, HEADER,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		Stopwatch sw = Stopwatch.createUnstarted();
		FileRepositoryBuilder builder = new FileRepositoryBuilder().setGitDir(gitDir.toFile()).readEnvironment();
		try (org.eclipse.jgit.lib.Repository repo = builder.build(); Git git = new Git(repo); RevWalk rw = new RevWalk(repo)) {
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
				makePristine(git);
				git.checkout()
					.setName(sha)
					.setForced(true)
					.call();
				Flags flags = changedJavaOrPom(repo, commit);
				long checkoutTime = sw.elapsed().toMillis();

				LOGGER.info("Commit {} on {}: {}", sha, date, msg);

				if (sources.stream().noneMatch(Files::exists)/* || poms.stream().noneMatch(Files::exists)*/) {
					LOGGER.info("Skipping.");
					continue;
				}

				long classpathTime = 0L;
				/*if (classpath.isEmpty() || flags.pomChanged()) {
					Path pom = poms.stream().filter(Files::exists).findFirst().get();
					sw.reset().start();
					List<Path> cp = maven.buildClasspath(pom);
					if (!cp.isEmpty()) {
						classpath = cp;
					} else {
						LOGGER.warn("Couldn't build classpath");
					}
					classpathTime = sw.elapsed().toMillis();
					LOGGER.info("Recomputing classpath took {}ms: {}", classpathTime, classpath);
				}*/

				Path src = sources.stream().filter(Files::exists).findFirst().get();
				sw.reset().start();
				API currentApi = buildApi(src, classpath);
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

					var line = "%s|%s|%s|%s|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%d|%s%n".formatted(
						sha, date, msg.replace("|", " "), url + "/commit/" + sha,
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

	static Flags changedJavaOrPom(org.eclipse.jgit.lib.Repository repo, RevCommit commit) {
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
		} catch (Exception e) {
			return new Flags(true, true);
		}
	}

	private static void makePristine(Git git) throws Exception {
		// Clears unmerged index entries and resets tracked files.
		git.reset()
			.setMode(ResetCommand.ResetType.HARD)
			.call();

		// Removes anything untracked/ignored that could block updates or violate “exact tree”.
		git.clean()
			.setCleanDirectories(true)
			.setIgnore(false)
			.call();
	}
}
