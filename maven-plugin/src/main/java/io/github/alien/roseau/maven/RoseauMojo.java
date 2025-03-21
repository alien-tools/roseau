package io.github.alien.roseau.maven;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.extractors.asm.AsmAPIExtractor;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.List;

@Mojo(name = "check")
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
		if (!builtJar.toFile().exists()) {
			getLog().warn("Built JAR file " + builtJar + " not found; skipping.");
			return;
		}

		Path oldVersionJar = resolveOldVersionJar();
		if (!oldVersionJar.toFile().exists()) {
			getLog().warn("Old version JAR file not found; skipping.");
			return;
		}

		List<BreakingChange> bcs = compare(builtJar, oldVersionJar);
		bcs.forEach(bc -> {
			getLog().warn(format(bc));
		});

		if (bcs.stream().anyMatch(bc -> bc.kind().isBinaryBreaking()) && failOnBinaryIncompatibility) {
			throw new MojoExecutionException("Binary incompatible changes found.");
		}

		if (bcs.stream().anyMatch(bc -> bc.kind().isSourceBreaking()) && failOnSourceIncompatibility) {
			throw new MojoExecutionException("Source incompatible changes found.");
		}
	}

	private List<BreakingChange> compare(Path oldJar, Path newJar) {
		API oldApi = buildAPI(oldJar);
		API newApi = buildAPI(newJar);

		return new APIDiff(oldApi, newApi).diff();
	}

	private API buildAPI(Path jar) {
		AsmAPIExtractor extractor = new AsmAPIExtractor();
		return extractor.extractAPI(jar);
	}

	private Path getBuiltJar() {
		return Path.of(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".jar");
	}

	private Path resolveOldVersionJar() throws MojoExecutionException {
		try {
			String coords = "%s:%s:%s".formatted(oldVersion.getGroupId(), oldVersion.getArtifactId(), oldVersion.getVersion());
			DefaultArtifact artifact = new DefaultArtifact(coords);
			ArtifactRequest request = new ArtifactRequest()
				.setArtifact(artifact)
				.setRepositories(remoteRepositories);

			ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
			return result.getArtifact().getFile().toPath();
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException("Error resolving old version JAR", e);
		}
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
