package io.github.alien.roseau.cli;

import io.github.alien.roseau.RoseauException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

final class ArtifactDownloader {
	private ArtifactDownloader() {
	}

	/**
	 * Downloads the artifact identified by the given coordinates from Maven Central and returns a
	 * path to a temporary copy of the JAR. The temporary file is registered for deletion on JVM
	 * exit; the Maven local repository used during resolution is deleted immediately after use to
	 * avoid polluting the file system.
	 */
	static Path downloadArtifact(ArtifactCoordinates coordinates) {
		RepositorySystem repoSystem = new RepositorySystemSupplier().get();
		Path localRepoDir = null;
		try {
			localRepoDir = Files.createTempDirectory("roseau-m2-");
			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			session.setLocalRepositoryManager(
				repoSystem.newLocalRepositoryManager(session, new LocalRepository(localRepoDir)));

			RemoteRepository central = new RemoteRepository.Builder("central", "default",
				"https://repo.maven.apache.org/maven2/").build();

			ArtifactResult result = repoSystem.resolveArtifact(session,
				new ArtifactRequest(
					new DefaultArtifact(coordinates.groupId(), coordinates.artifactId(),
						coordinates.classifier(), coordinates.extension(), coordinates.version()),
					List.of(central), null));

			// Copy the resolved artifact to a standalone temp file so the Maven local repository
			// can be safely deleted below without affecting downstream consumers.
			Path tempJar = Files.createTempFile("roseau-artifact-", ".jar");
			tempJar.toFile().deleteOnExit();
			Files.copy(result.getArtifact().getPath(), tempJar, StandardCopyOption.REPLACE_EXISTING);
			return tempJar;
		} catch (Exception e) {
			throw new RoseauException(
				"Failed to download %s:%s:%s".formatted(
					coordinates.groupId(), coordinates.artifactId(), coordinates.version()), e);
		} finally {
			repoSystem.shutdown();
			deleteRecursively(localRepoDir);
		}
	}

	private static void deleteRecursively(Path path) {
		if (path == null) {
			return;
		}
		try (var stream = Files.walk(path)) {
			stream.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}
	}
}
