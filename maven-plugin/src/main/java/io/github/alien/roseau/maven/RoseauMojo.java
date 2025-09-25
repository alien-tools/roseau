package io.github.alien.roseau.maven;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.diff.changes.BreakingChange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class RoseauMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(property = "roseau.skip", defaultValue = "false")
	private boolean skip;

	@Parameter(property = "roseau.failOnBinaryIncompatibility", defaultValue = "false")
	private boolean failOnBinaryIncompatibility;

	@Parameter(property = "roseau.failOnSourceIncompatibility", defaultValue = "false")
	private boolean failOnSourceIncompatibility;

	@Parameter(property = "roseau.oldVersion")
	private Dependency oldVersion;

	@Parameter(property = "roseau.oldJar")
	private Path oldJar;

	@Inject
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> remoteRepositories;

	@Override
	public void execute() throws MojoExecutionException {
		if (skip) {
			getLog().info("Skipping.");
			return;
		}

		if (oldVersion == null) {
			getLog().warn("No oldVersion specified; skipping.");
			return;
		}

		Path builtJar = getBuiltJar();
		if (!Files.isRegularFile(builtJar)) {
			getLog().warn("Built JAR file " + builtJar + " not found; skipping.");
			return;
		}

		if (oldVersion.getArtifactId() != null) {
			Optional<Path> opt = resolveOldVersionJar();
			if (opt.isPresent()) {
				getLog().info("Baseline version is " + opt.get());
				check(opt.get(), builtJar);
			} else {
				getLog().warn("Couldn't resolve the old version; skipping.");
			}
		} else if (Files.isRegularFile(oldJar)) {
			getLog().info("Baseline version is " + oldJar);
			check(oldJar, builtJar);
		} else {
			getLog().warn("No baseline version specified; skipping.");
		}
	}

	private void check(Path oldJar, Path newJar) throws MojoExecutionException {
		List<BreakingChange> bcs = diff(oldJar, newJar);
		if (bcs.isEmpty()) {
			getLog().info("No breaking changes found.");
			return;
		}
		bcs.forEach(bc -> {
			getLog().warn(format(bc));
		});

		if (failOnBinaryIncompatibility && bcs.stream().anyMatch(bc -> bc.kind().isBinaryBreaking())) {
			throw new MojoExecutionException("Binary incompatible changes found.");
		}

		if (failOnSourceIncompatibility && bcs.stream().anyMatch(bc -> bc.kind().isSourceBreaking())) {
			throw new MojoExecutionException("Source incompatible changes found.");
		}
	}

	private List<BreakingChange> diff(Path oldJar, Path newJar) {
		Library oldLibrary = Library.of(oldJar);
		Library newLibrary = Library.of(newJar);

		return Roseau.diff(oldLibrary, newLibrary).breakingChanges();
	}

	private Path getBuiltJar() throws MojoExecutionException {
		var artifact = project.getArtifact();
		var jar = artifact.getFile();
		if (jar == null) {
			throw new MojoExecutionException(
				"Project artifact is not packaged yet. Bind this goal to 'package' or a later phase.");
		}
		return jar.toPath();
	}

	private Optional<Path> resolveOldVersionJar() {
		try {
			String coords = "%s:%s:%s".formatted(oldVersion.getGroupId(), oldVersion.getArtifactId(), oldVersion.getVersion());
			Artifact artifact = new DefaultArtifact(coords);
			ArtifactRequest request = new ArtifactRequest()
				.setArtifact(artifact)
				.setRepositories(remoteRepositories);

			ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
			File resolved = result.getArtifact().getFile();
			if (resolved != null && Files.isRegularFile(resolved.toPath())) {
				return Optional.empty();
			}
		} catch (ArtifactResolutionException e) {
			// shh
		}

		return Optional.empty();
	}

	private String format(BreakingChange bc) {
		return String.format("%s %s%n\t%s:%s",
			RED_TEXT + BOLD + bc.kind() + RESET,
			UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
			bc.impactedSymbol().getLocation().file(),
			bc.impactedSymbol().getLocation().line());
	}

	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";
}
