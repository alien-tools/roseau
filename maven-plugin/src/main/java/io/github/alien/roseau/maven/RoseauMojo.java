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

@Mojo(
	name = "check",
	defaultPhase = LifecyclePhase.VERIFY,
	threadSafe = true,
	requiresOnline = true
)
public final class RoseauMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;
	@Parameter(property = "roseau.skip", defaultValue = "false")
	private boolean skip;
	@Parameter(property = "roseau.failOnIncompatibility", defaultValue = "false")
	private boolean failOnIncompatibility;
	@Parameter(property = "roseau.failOnBinaryIncompatibility", defaultValue = "false")
	private boolean failOnBinaryIncompatibility;
	@Parameter(property = "roseau.failOnSourceIncompatibility", defaultValue = "false")
	private boolean failOnSourceIncompatibility;
	@Parameter(property = "roseau.baselineVersion")
	private Dependency baselineVersion;
	@Parameter(property = "roseau.baselineJar")
	private Path baselineJar;
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

		if (project.getPackaging().equals("pom")) {
			getLog().info("Packaging of the project is 'pom'; skipping.");
			return;
		}

		if (baselineVersion.getArtifactId() == null && baselineJar == null) {
			getLog().error("No baseline specified; skipping.");
			return;
		}

		Optional<Path> maybeJar = resolveArtifactJar();
		if (maybeJar.isEmpty()) {
			getLog().error("Current artifact " + maybeJar + " not found; skipping." +
				" Make sure that the artifact was built in the 'package' phase.");
			return;
		}

		if (baselineVersion.getArtifactId() != null) {
			Optional<Path> maybeBaseline = resolveBaselineVersion();
			if (maybeBaseline.isPresent()) {
				check(maybeBaseline.get(), maybeJar.get());
			} else {
				getLog().error("Couldn't resolve the baseline version; skipping.");
			}
		} else if (baselineJar != null) {
			if (Files.isRegularFile(baselineJar)) {
				check(baselineJar, maybeJar.get());
			} else {
				getLog().error("Invalid baseline JAR " + baselineJar);
			}
		} else {
			getLog().error("No baseline version specified; skipping.");
		}
	}

	private void check(Path oldJar, Path newJar) throws MojoExecutionException {
		List<BreakingChange> bcs = diff(oldJar, newJar);
		if (bcs.isEmpty()) {
			getLog().info("No breaking changes found.");
			return;
		} else {
			bcs.forEach(bc -> getLog().warn(format(bc)));
		}

		if (failOnIncompatibility && !bcs.isEmpty()) {
			throw new MojoExecutionException("Breaking changes found; failing.");
		}

		if (failOnBinaryIncompatibility && bcs.stream().anyMatch(bc -> bc.kind().isBinaryBreaking())) {
			throw new MojoExecutionException("Binary incompatible changes found; failing.");
		}

		if (failOnSourceIncompatibility && bcs.stream().anyMatch(bc -> bc.kind().isSourceBreaking())) {
			throw new MojoExecutionException("Source incompatible changes found; failing.");
		}
	}

	private List<BreakingChange> diff(Path oldJar, Path newJar) {
		Library oldLibrary = Library.of(oldJar);
		Library newLibrary = Library.of(newJar);

		return Roseau.diff(oldLibrary, newLibrary).getBreakingChanges();
	}

	private Optional<Path> resolveArtifactJar() {
		org.apache.maven.artifact.Artifact artifact = project.getArtifact();
		File jar = artifact.getFile();
		if (jar != null && Files.isRegularFile(jar.toPath())) {
			return Optional.of(jar.toPath());
		} else {
			return Optional.empty();
		}
	}

	private Optional<Path> resolveBaselineVersion() {
		try {
			String coordinates = "%s:%s:%s".formatted(baselineVersion.getGroupId(), baselineVersion.getArtifactId(),
				baselineVersion.getVersion());
			Artifact artifact = new DefaultArtifact(coordinates);
			ArtifactRequest request = new ArtifactRequest()
				.setArtifact(artifact)
				.setRepositories(remoteRepositories);

			ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
			File resolved = result.getArtifact().getFile();
			if (resolved != null && Files.isRegularFile(resolved.toPath())) {
				return Optional.of(resolved.toPath());
			}
		} catch (ArtifactResolutionException e) {
			getLog().warn(e);
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
