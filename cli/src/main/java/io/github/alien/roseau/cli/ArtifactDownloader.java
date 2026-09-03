package io.github.alien.roseau.cli;

import com.google.common.io.MoreFiles;
import io.github.alien.roseau.RoseauException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

final class ArtifactDownloader {
	private static final RemoteRepository CENTRAL = new RemoteRepository.Builder(
		"central", "default", "https://repo.maven.apache.org/maven2/").build();

	private ArtifactDownloader() {
	}

	record Resolution(Path artifact, List<Path> classpath) {
		Resolution {
			classpath = List.copyOf(classpath);
		}
	}

	static Resolution resolveArtifact(ArtifactCoordinates coordinates) {
		return resolveArtifact(coordinates, List.of(CENTRAL));
	}

	static Resolution resolveArtifact(ArtifactCoordinates coordinates, List<RemoteRepository> repositories) {
		RepositorySystem repoSystem = new RepositorySystemSupplier().get();
		Path localRepoDir = null;
		try {
			localRepoDir = Files.createTempDirectory("roseau-m2-");
			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			session.setLocalRepositoryManager(
				repoSystem.newLocalRepositoryManager(session, new LocalRepository(localRepoDir)));

			Artifact requestedArtifact = new DefaultArtifact(coordinates.groupId(), coordinates.artifactId(),
				coordinates.classifier(), coordinates.extension(), coordinates.version());
			ArtifactResult artifactResult = repoSystem.resolveArtifact(session,
				new ArtifactRequest(requestedArtifact, repositories, null));
			DependencyResult dependencyResult = repoSystem.resolveDependencies(session, new DependencyRequest(
				new CollectRequest(new Dependency(requestedArtifact, JavaScopes.COMPILE), repositories),
				DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME)));

			Path artifact = copyToTemp(artifactResult.getArtifact().getPath());
			List<Path> classpath = new ArrayList<>();
			for (ArtifactResult dependencyResultEntry : dependencyResult.getArtifactResults()) {
				Artifact dependency = dependencyResultEntry.getArtifact();
				if (!hasSameCoordinates(dependency, artifactResult.getArtifact()) &&
					"jar".equals(dependency.getExtension())) {
					classpath.add(copyToTemp(dependency.getPath()));
				}
			}
			return new Resolution(artifact, classpath);
		} catch (Exception e) {
			throw new RoseauException(
				"Failed to download %s:%s:%s".formatted(
					coordinates.groupId(), coordinates.artifactId(), coordinates.version()), e);
		} finally {
			repoSystem.shutdown();
			try {
				if (localRepoDir != null) {
					MoreFiles.deleteRecursively(localRepoDir);
				}
			} catch (IOException _) {
				// shh
			}
		}
	}

	private static boolean hasSameCoordinates(Artifact left, Artifact right) {
		return left.getGroupId().equals(right.getGroupId()) &&
			left.getArtifactId().equals(right.getArtifactId()) &&
			left.getVersion().equals(right.getVersion()) &&
			left.getExtension().equals(right.getExtension()) &&
			left.getClassifier().equals(right.getClassifier());
	}

	private static Path copyToTemp(Path artifact) throws IOException {
		Path copy = Files.createTempFile("roseau-artifact-", "-" + artifact.getFileName());
		copy.toFile().deleteOnExit();
		return Files.copy(artifact, copy, StandardCopyOption.REPLACE_EXISTING);
	}
}
