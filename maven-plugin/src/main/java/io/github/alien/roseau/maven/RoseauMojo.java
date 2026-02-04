package io.github.alien.roseau.maven;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mojo(
	name = "check",
	defaultPhase = LifecyclePhase.VERIFY,
	threadSafe = true,
	requiresOnline = true,
	requiresDependencyResolution = ResolutionScope.COMPILE
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
	@Parameter
	private List<Path> classpath;
	@Parameter
	private Path classpathPom;
	@Parameter
	private List<Path> baselineClasspath;
	@Parameter
	private Path baselineClasspathPom;
	@Parameter
	private List<String> excludeNames;
	@Parameter
	private List<AnnotationExclusion> excludeAnnotations;
	@Parameter(property = "roseau.binaryOnly", defaultValue = "false")
	private boolean binaryOnly;
	@Parameter(property = "roseau.sourceOnly", defaultValue = "false")
	private boolean sourceOnly;
	@Parameter(property = "roseau.ignoredCsv")
	private Path ignoredCsv;
	@Parameter
	private List<ReportConfig> reports;
	@Parameter(property = "roseau.reportDirectory")
	private File reportDirectory;
	@Parameter(property = "roseau.exportBaselineApi")
	private Path exportBaselineApi;
	@Parameter(property = "roseau.exportCurrentApi")
	private Path exportCurrentApi;
	@Parameter(property = "roseau.configFile", defaultValue = "${project.basedir}/roseau.yaml")
	private Path configFile;
	@Parameter(property = "roseau.useConfigFile", defaultValue = "true")
	private boolean useConfigFile;
	@Parameter(property = "roseau.verbosity")
	private String verbosity;

	@Inject
	private RepositorySystem repositorySystem;
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;
	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> remoteRepositories;

	/**
	 * Configures logging based on the verbosity parameter.
	 */
	private void configureLogging() {
		if (verbosity == null || verbosity.isEmpty()) {
			return;
		}

		Level level = switch (verbosity.toUpperCase(Locale.ROOT)) {
			case "QUIET" -> Level.ERROR;
			case "NORMAL" -> Level.WARN;
			case "VERBOSE" -> Level.INFO;
			case "DEBUG" -> Level.DEBUG;
			default -> {
				getLog().warn("Invalid verbosity level: " + verbosity + ". Valid values: QUIET, NORMAL, VERBOSE, DEBUG");
				yield null;
			}
		};

		if (level != null) {
			try {
				LoggerContext context = (LoggerContext) LogManager.getContext(false);
				Configuration config = context.getConfiguration();
				LoggerConfig loggerConfig = config.getLoggerConfig("io.github.alien.roseau");
				loggerConfig.setLevel(level);
				context.updateLoggers();
				getLog().debug("Set Roseau logging level to " + level);
			} catch (Exception e) {
				getLog().warn("Could not configure logging: " + e.getMessage());
			}
		}
	}

	/**
	 * Exports API models to JSON files.
	 *
	 * @param report the RoseauReport containing the APIs
	 * @throws MojoExecutionException if an error occurs while exporting APIs
	 */
	private void exportApis(RoseauReport report) throws MojoExecutionException {
		if (exportBaselineApi != null) {
			try {
				Path exportPath = exportBaselineApi;
				if (!exportPath.isAbsolute()) {
					exportPath = project.getBasedir().toPath().resolve(exportPath);
				}

				Path parentDir = exportPath.getParent();
				if (parentDir != null && !Files.exists(parentDir)) {
					Files.createDirectories(parentDir);
				}

				report.v1().getLibraryTypes().writeJson(exportPath);
				getLog().info("Baseline API exported to " + exportPath);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to export baseline API to " + exportBaselineApi, e);
			}
		}

		if (exportCurrentApi != null) {
			try {
				Path exportPath = exportCurrentApi;
				if (!exportPath.isAbsolute()) {
					exportPath = project.getBasedir().toPath().resolve(exportPath);
				}

				Path parentDir = exportPath.getParent();
				if (parentDir != null && !Files.exists(parentDir)) {
					Files.createDirectories(parentDir);
				}

				report.v2().getLibraryTypes().writeJson(exportPath);
				getLog().info("Current API exported to " + exportPath);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to export current API to " + exportCurrentApi, e);
			}
		}
	}

	/**
	 * Writes reports to configured output files.
	 *
	 * @param report the RoseauReport to format and write
	 * @throws MojoExecutionException if an error occurs while writing reports
	 */
	private void writeReports(RoseauReport report) throws MojoExecutionException {
		if (reports == null || reports.isEmpty()) {
			return;
		}

		// Determine the base directory for reports
		// Default to ${project.build.directory}/roseau if not specified
		Path baseDir;
		if (reportDirectory != null) {
			Path reportDirectory = this.reportDirectory.toPath();
			baseDir = reportDirectory.isAbsolute()
				? reportDirectory
				: project.getBasedir().toPath().resolve(reportDirectory);
			getLog().debug("Using configured report directory: " + baseDir);
		} else {
			// Use project.build.directory + "/roseau" as default
			File buildDir = new File(project.getBuild().getDirectory());
			baseDir = buildDir.toPath().resolve("roseau");
			getLog().debug("Using default report directory: " + baseDir + " (build dir: " + project.getBuild().getDirectory() + ")");
		}

		for (ReportConfig reportConfig : reports) {
			try {
				getLog().debug("Processing report config: file=" + reportConfig.file + ", isAbsolute=" + reportConfig.file.isAbsolute());
				getLog().debug("Base directory for reports: " + baseDir);

				Path reportFile = reportConfig.file.isAbsolute()
					? reportConfig.file
					: baseDir.resolve(reportConfig.file);

				getLog().debug("Final report file path: " + reportFile);

				// Create parent directories if they don't exist
				Path parentDir = reportFile.getParent();
				if (parentDir != null && !Files.exists(parentDir)) {
					Files.createDirectories(parentDir);
				}

				// Get formatter and format report
				RoseauOptions.Report roseauReport = reportConfig.toRoseauReport();
				BreakingChangesFormatter formatter = BreakingChangesFormatterFactory.newBreakingChangesFormatter(roseauReport.format());
				String formattedReport = formatter.format(report);

				// Write to file
				Files.writeString(reportFile, formattedReport);

				getLog().info("Report written to " + reportFile);
			} catch (IOException e) {
				throw new MojoExecutionException("Failed to write report to " + reportConfig.file, e);
			} catch (IllegalArgumentException e) {
				throw new MojoExecutionException("Invalid report format: " + reportConfig.format, e);
			}
		}
	}

	/**
	 * Loads the complete configuration, merging YAML config file with Maven parameters.
	 * Maven parameters take precedence over YAML configuration.
	 *
	 * @param oldJar the path to the baseline JAR
	 * @param newJar the path to the current JAR
	 * @return the final RoseauOptions instance
	 */
	private RoseauOptions loadConfiguration(Path oldJar, Path newJar) {
		// Start with default configuration
		RoseauOptions options = RoseauOptions.newDefault();

		// Load and merge YAML configuration if enabled and exists
		if (useConfigFile && configFile != null && Files.isRegularFile(configFile)) {
			try {
				RoseauOptions yamlOptions = RoseauOptions.load(configFile);
				options = options.mergeWith(yamlOptions);
				getLog().debug("Loaded configuration from " + configFile);
			} catch (Exception e) {
				getLog().warn("Could not load configuration file " + configFile + ": " + e.getMessage());
			}
		}

		// Build and merge Maven configuration (Maven takes precedence)
		RoseauOptions mavenOptions = buildMavenOptions(oldJar, newJar);
		getLog().debug("mavenOptions = " + mavenOptions);
		options = options.mergeWith(mavenOptions);
		getLog().debug("Roseau options = " + options);

		return options;
	}

	/**
	 * Builds RoseauOptions from Maven configuration parameters.
	 *
	 * @param oldJar the path to the baseline JAR
	 * @param newJar the path to the current JAR
	 * @return the RoseauOptions instance
	 */
	private RoseauOptions buildMavenOptions(Path oldJar, Path newJar) {
		// Resolve project classpath if enabled
		List<Path> projectClasspath = resolveProjectClasspath();

		// Merge with manual classpath
		List<Path> mergedClasspath = new ArrayList<>();
		if (classpath != null && !classpath.isEmpty()) {
			mergedClasspath.addAll(classpath);
		}
		mergedClasspath.addAll(projectClasspath);

		// Resolve baseline classpath
		List<Path> baselineClasspathResolved = new ArrayList<>();
		if (baselineClasspath != null && !baselineClasspath.isEmpty()) {
			baselineClasspathResolved.addAll(baselineClasspath);
		}
		baselineClasspathResolved.addAll(resolveBaselineClasspath());

		// Build Common configuration (classpath + exclusions)
		RoseauOptions.Classpath commonClasspath = new RoseauOptions.Classpath(
			classpathPom,
			mergedClasspath
		);

		RoseauOptions.Exclude commonExclude = new RoseauOptions.Exclude(
			excludeNames != null ? excludeNames : List.of(),
			excludeAnnotations != null
				? excludeAnnotations.stream()
				.map(AnnotationExclusion::toRoseauAnnotationExclusion)
				.collect(Collectors.toList())
				: List.of()
		);

		RoseauOptions.Common common = new RoseauOptions.Common(commonClasspath, commonExclude);

		// Build Library v1 (baseline)
		RoseauOptions.Classpath v1Classpath = new RoseauOptions.Classpath(
			baselineClasspathPom,
			baselineClasspathResolved
		);
		RoseauOptions.Library v1 = new RoseauOptions.Library(
			oldJar,
			v1Classpath,
			new RoseauOptions.Exclude(List.of(), List.of()),
			exportBaselineApi
		);

		// Build Library v2 (current)
		RoseauOptions.Library v2 = new RoseauOptions.Library(
			newJar,
			new RoseauOptions.Classpath(null, List.of()),
			new RoseauOptions.Exclude(List.of(), List.of()),
			exportCurrentApi
		);

		// Build Diff configuration
		RoseauOptions.Diff diff = new RoseauOptions.Diff(
			ignoredCsv,
			sourceOnly,
			binaryOnly
		);

		// Build Reports list
		List<RoseauOptions.Report> reportsList = reports != null
			? reports.stream()
			.map(ReportConfig::toRoseauReport)
			.collect(Collectors.toList())
			: List.of();

		return new RoseauOptions(common, v1, v2, diff, reportsList);
	}

	/**
	 * Resolves the project's compile and runtime dependencies from the classpath.
	 *
	 * @return a list of paths to the dependency JARs
	 */
	private List<Path> resolveProjectClasspath() {
		return project.getArtifacts().stream()
			.filter(artifact -> "compile".equals(artifact.getScope()))
			.map(org.apache.maven.artifact.Artifact::getFile)
			.filter(file -> file != null && Files.isRegularFile(file.toPath()))
			.map(File::toPath)
			.toList();
	}

	/**
	 * Resolves the baseline artifact and its transitive dependencies.
	 *
	 * @return a list of paths to the baseline dependency JARs, or an empty list if resolution fails
	 */
	private List<Path> resolveBaselineClasspath() {
		if (baselineVersion == null || baselineVersion.getArtifactId() == null) {
			return List.of();
		}

		try {
			String coordinates = "%s:%s:%s".formatted(
				baselineVersion.getGroupId(),
				baselineVersion.getArtifactId(),
				baselineVersion.getVersion()
			);
			Artifact artifact = new DefaultArtifact(coordinates);

			// Create a dependency request with transitive resolution
			DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

			CollectRequest collectRequest = new CollectRequest()
				.setRoot(new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.COMPILE))
				.setRepositories(remoteRepositories);

			DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);

			// Resolve dependencies
			var result = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);

			return result.getArtifactResults().stream()
				.map(ArtifactResult::getArtifact)
				.map(Artifact::getFile)
				.filter(file -> file != null && Files.isRegularFile(file.toPath()))
				.map(File::toPath)
				.toList();
		} catch (DependencyResolutionException e) {
			getLog().warn("Could not resolve baseline dependencies: " + e.getMessage());
			return List.of();
		}
	}

	@Override
	public void execute() throws MojoExecutionException {
		// Configure logging first
		configureLogging();

		if (skip) {
			getLog().info("Skipping.");
			return;
		}

		if (project.getPackaging().equals("pom")) {
			getLog().info("Packaging of the project is 'pom'; skipping.");
			return;
		}

		if ((baselineVersion == null || baselineVersion.getArtifactId() == null) && baselineJar == null) {
			getLog().error("No baseline specified; skipping.");
			return;
		}

		Optional<Path> maybeJar = resolveArtifactJar();
		if (maybeJar.isEmpty()) {
			getLog().error("Current artifact not found; skipping." +
				" Make sure that the artifact was built in the 'package' phase.");
			return;
		}

		if (baselineVersion != null && baselineVersion.getArtifactId() != null) {
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
		// Load configuration (YAML + Maven parameters)
		RoseauOptions options = loadConfiguration(oldJar, newJar);

		// Build libraries with configuration
		Library oldLibrary = options.v1().mergeWith(options.common()).toLibrary();
		Library newLibrary = options.v2().mergeWith(options.common()).toLibrary();

		// Run diff
		RoseauReport report = Roseau.diff(oldLibrary, newLibrary);

		// Export APIs if configured
		exportApis(report);

		// Filter report based on configuration
		RoseauReport filteredReport = report.filterReport(options.diff());

		// Write reports to files if configured
		writeReports(filteredReport);

		// Get breaking changes for display and fail checks
		List<BreakingChange> bcs = filteredReport.getBreakingChanges();

		if (bcs.isEmpty()) {
			getLog().info("No breaking changes found.");
			return;
		} else {
			BreakingChangesFormatter formatter = new MavenFormatter(getLog());
			formatter.format(filteredReport);
		}

		// Fail checks
		if (failOnIncompatibility) {
			throw new MojoExecutionException("Breaking changes found; failing.");
		}

		if (failOnBinaryIncompatibility && filteredReport.isBinaryBreaking()) {
			throw new MojoExecutionException("Binary incompatible changes found; failing.");
		}

		if (failOnSourceIncompatibility && filteredReport.isSourceBreaking()) {
			throw new MojoExecutionException("Source incompatible changes found; failing.");
		}
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

	/**
	 * Configuration for annotation-based exclusions.
	 */
	public static class AnnotationExclusion {
		@Parameter(required = true)
		private String name;
		@Parameter
		private Map<String, String> args;

		/**
		 * Converts this Maven configuration to a Roseau AnnotationExclusion.
		 *
		 * @return the Roseau AnnotationExclusion instance
		 */
		public RoseauOptions.AnnotationExclusion toRoseauAnnotationExclusion() {
			return new RoseauOptions.AnnotationExclusion(name, args != null ? args : Map.of());
		}
	}

	/**
	 * Configuration for report generation.
	 */
	public static class ReportConfig {
		@Parameter(required = true)
		private Path file;
		@Parameter(required = true)
		private String format;

		/**
		 * Converts this Maven configuration to a Roseau Report.
		 *
		 * @return the Roseau Report instance
		 */
		public RoseauOptions.Report toRoseauReport() {
			return new RoseauOptions.Report(file,
				BreakingChangesFormatterFactory.valueOf(format.toUpperCase(Locale.ROOT)));
		}
	}
}
