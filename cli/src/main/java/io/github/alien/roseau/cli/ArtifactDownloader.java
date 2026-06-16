package io.github.alien.roseau.cli;

import com.google.common.io.MoreFiles;
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
import java.util.List;

final class ArtifactDownloader {
	private ArtifactDownloader() {}

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
			try {
				if (localRepoDir != null) {
					MoreFiles.deleteRecursively(localRepoDir);
				}
			} catch (IOException _) {
				// shh
			}
		}
	}
}
