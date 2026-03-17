package io.github.alien.roseau.maven;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Compares the current module artifact with a baseline artifact and reports API breaking changes.
 * <p>
 * The baseline can be provided as Maven coordinates ({@code baselineCoordinates} or {@code baselineDependency})
 * or as a local file ({@code baselineJar}). {@code baselineCoordinates} takes precedence over
 * {@code baselineDependency}, which in turn takes precedence over {@code baselineJar}.
 * When reports are configured, report files are written under {@code reportDirectory}.
 */
@Mojo(
	name = "check",
	defaultPhase = LifecyclePhase.VERIFY,
	threadSafe = true,
	requiresOnline = true,
	requiresDependencyResolution = ResolutionScope.COMPILE
)
public final class RoseauMojo extends AbstractMojo {
	/**
	 * Current Maven project.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	/**
	 * Skips plugin execution.
	 */
	@Parameter(property = "roseau.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * Skips the check when the baseline artifact cannot be resolved from Maven repositories.
	 */
	@Parameter(property = "roseau.skipIfBaselineUnresolvable", defaultValue = "false")
	private boolean skipIfBaselineUnresolvable;

	/**
	 * Reports only binary-breaking changes.
	 */
	@Parameter(property = "roseau.binaryOnly")
	private Boolean binaryOnly;

	/**
	 * Reports only source-breaking changes.
	 */
	@Parameter(property = "roseau.sourceOnly")
	private Boolean sourceOnly;

	/**
	 * Fails the build when any breaking change is found.
	 */
	@Parameter(property = "roseau.failOnIncompatibility", defaultValue = "false")
	private boolean failOnIncompatibility;

	/**
	 * Fails the build when any binary-breaking change is found.
	 */
	@Parameter(property = "roseau.failOnBinaryIncompatibility", defaultValue = "false")
	private boolean failOnBinaryIncompatibility;

	/**
	 * Fails the build when any source-breaking change is found.
	 */
	@Parameter(property = "roseau.failOnSourceIncompatibility", defaultValue = "false")
	private boolean failOnSourceIncompatibility;

	/**
	 * Baseline artifact coordinates as a string ({@code groupId:artifactId:version[:extension[:classifier]]})
	 * resolved from Maven repositories. Takes precedence over {@code baselineDependency} when set.
	 */
	@Parameter(property = "roseau.baselineCoordinates")
	private String baselineCoordinates;

	/**
	 * Baseline artifact coordinates as structured Maven dependency resolved from Maven repositories.
	 */
	@Parameter
	private Dependency baselineDependency;

	/**
	 * Baseline artifact file path, used when neither {@code baselineCoordinates} nor
	 * {@code baselineDependency} is provided.
	 */
	@Parameter(property = "roseau.baselineJar")
	private Path baselineJar;

	/**
	 * Additional classpath entries shared by both baseline and current artifacts.
	 */
	@Parameter
	private List<Path> classpath;

	/**
	 * POM file used to derive classpath entries shared by both baseline and current artifacts.
	 */
	@Parameter
	private Path classpathPom;

	/**
	 * Additional classpath entries used only for the baseline artifact.
	 */
	@Parameter
	private List<Path> baselineClasspath;

	/**
	 * POM file used to derive classpath entries for the baseline artifact.
	 */
	@Parameter
	private Path baselineClasspathPom;

	/**
	 * Report files to generate (for example CSV, HTML).
	 */
	@Parameter
	private List<ReportConfig> reports;

	/**
	 * Output directory for relative report file paths.
	 */
	@Parameter(property = "roseau.reportDirectory", defaultValue = "${project.build.directory}/roseau")
	private File reportDirectory;

	/**
	 * Optional path where to export the baseline API model as JSON.
	 */
	@Parameter(property = "roseau.exportBaselineApi")
	private Path exportBaselineApi;

	/**
	 * Optional path where to export the current API model as JSON.
	 */
	@Parameter(property = "roseau.exportCurrentApi")
	private Path exportCurrentApi;

	/**
	 * Optional Roseau YAML configuration file.
	 */
	@Parameter(property = "roseau.configFile")
	private Path configFile;

	/**
	 * Logging verbosity for Roseau internals: QUIET, NORMAL, VERBOSE, DEBUG.
	 */
	@Parameter(property = "roseau.verbosity")
	private String verbosity;

	/**
	 * Maven artifact resolver.
	 */
	@Inject
	private RepositorySystem repositorySystem;

	/**
	 * Current Maven resolver session.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	/**
	 * Remote repositories available for baseline resolution.
	 */
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
			exportApi(report.v1(), exportBaselineApi);
		}

		if (exportCurrentApi != null) {
			exportApi(report.v2(), exportCurrentApi);
		}
	}

	private void exportApi(API api, Path path) throws MojoExecutionException {
		try {
			Path exportPath = path.isAbsolute() ? path : project.getBasedir().toPath().resolve(path);
			makeParent(exportPath);
			api.getLibraryTypes().writeJson(exportPath);
			getLog().info("API exported to " + exportPath);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to export API to " + path, e);
		}
	}

	/**
	 * Writes reports to configured output files.
	 *
	 * @param report the RoseauReport to format and write
	 */
	private void writeReports(RoseauReport report, List<RoseauOptions.Report> reportConfigs) {
		if (reportConfigs == null || reportConfigs.isEmpty()) {
			return;
		}

		for (RoseauOptions.Report config : reportConfigs) {
			Path outputPath = resolveReportPath(config.file());
			try {
				makeParent(outputPath);
				report.writeReport(config.format(), outputPath);
				getLog().info(String.format("%s report written to %s", config.format(), outputPath));
			} catch (IOException e) {
				getLog().error(String.format("Failed to write %s report to %s: %s",
					config.format(), outputPath, e.getMessage()));
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
	 * @throws MojoExecutionException if YAML file cannot be loaded
	 */
	private RoseauOptions loadConfiguration(Path oldJar, Path newJar) throws MojoExecutionException {
		// Start with default configuration
		RoseauOptions options = RoseauOptions.newDefault();
		Path resolvedConfigFile = resolvePath(configFile);

		// Load and merge YAML configuration if enabled
		if (resolvedConfigFile != null) {
			try {
				RoseauOptions yamlOptions = RoseauOptions.load(resolvedConfigFile);
				options = options.mergeWith(yamlOptions);
				getLog().info("Loaded configuration from " + resolvedConfigFile);
			} catch (RoseauException e) {
				throw new MojoExecutionException("Could not load configuration file " + resolvedConfigFile, e);
			}
		}

		// Load and merge Maven configuration
		RoseauOptions mavenOptions = buildMavenOptions(oldJar, newJar);
		options = options.mergeWith(mavenOptions);
		options = normalizePaths(options);

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
			mergedClasspath.addAll(resolvePaths(classpath));
		}
		mergedClasspath.addAll(projectClasspath);

		// Resolve baseline classpath
		List<Path> baselineClasspathResolved = new ArrayList<>();
		if (baselineClasspath != null && !baselineClasspath.isEmpty()) {
			baselineClasspathResolved.addAll(resolvePaths(baselineClasspath));
		}
		baselineClasspathResolved.addAll(resolveBaselineClasspath());

		// Build Common configuration (classpath + exclusions)
		RoseauOptions.Classpath commonClasspath = new RoseauOptions.Classpath(resolvePath(classpathPom), mergedClasspath);
		RoseauOptions.Exclude commonExclude = new RoseauOptions.Exclude(List.of(), List.of());
		RoseauOptions.Common common = new RoseauOptions.Common(commonClasspath, commonExclude);

		// Build Library v1 (baseline)
		RoseauOptions.Classpath v1Classpath = new RoseauOptions.Classpath(
			resolvePath(baselineClasspathPom), baselineClasspathResolved);
		RoseauOptions.Library v1 = new RoseauOptions.Library(
			oldJar, v1Classpath, new RoseauOptions.Exclude(List.of(), List.of()), resolvePath(exportBaselineApi));

		// Build Library v2 (current)
		RoseauOptions.Classpath v2Classpath = new RoseauOptions.Classpath(null, List.of());
		RoseauOptions.Library v2 = new RoseauOptions.Library(
			newJar, v2Classpath, new RoseauOptions.Exclude(List.of(), List.of()), resolvePath(exportCurrentApi));

		RoseauOptions.Diff diff = new RoseauOptions.Diff(null, sourceOnly, binaryOnly);

		// Build Reports list
		List<RoseauOptions.Report> reportsList = reports != null
			? reports.stream()
			.map(ReportConfig::toRoseauReport)
			.filter(Objects::nonNull)
			.toList()
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
		if (baselineDependency == null || baselineDependency.getArtifactId() == null) {
			return List.of();
		}

		try {
			Artifact artifact = new DefaultArtifact(toCoordinates(baselineDependency));

			// Create a dependency request with transitive resolution
			DependencyFilter filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

			CollectRequest collectRequest = new CollectRequest()
				.setRoot(new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.COMPILE))
				.setRepositories(remoteRepositories);

			DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);

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
		configureLogging();

		if (skip) {
			getLog().info("Skipping.");
			return;
		}

		if (project.getPackaging().equals("pom")) {
			getLog().info("Packaging of the project is 'pom'; skipping.");
			return;
		}

		if (baselineCoordinates != null && !baselineCoordinates.isBlank()) {
			baselineDependency = parseBaselineCoordinates(baselineCoordinates);
		}

		if ((baselineDependency == null || baselineDependency.getArtifactId() == null) && baselineJar == null) {
			throw new MojoExecutionException(
				"No baseline specified; configure baselineCoordinates, baselineDependency, or baselineJar.");
		}

		Optional<Path> maybeJar = resolveArtifactJar();
		if (maybeJar.isEmpty()) {
			throw new MojoExecutionException("Current artifact JAR not found. " +
				"Run 'mvn package roseau:check' or bind roseau to the verify phase.");
		}

		if (isBaselineDependencyConfigured()) {
			if (!isBaselineDependencyValid()) {
				throw new MojoExecutionException("Invalid baseline version coordinates; " +
					"groupId, artifactId and version are required.");
			}

			Optional<Path> maybeBaseline = resolveBaselineDependency();
			if (maybeBaseline.isPresent()) {
				check(maybeBaseline.get(), maybeJar.get());
			} else if (skipIfBaselineUnresolvable) {
				getLog().warn("Baseline could not be resolved; skipping check.");
			} else {
				throw new MojoExecutionException("Couldn't resolve the baseline version. " +
					"Set roseau.skipIfBaselineUnresolvable=true to skip the check instead of failing ");
			}
		} else if (baselineJar != null) {
			Path resolvedBaselineJar = resolvePath(baselineJar);
			if (Files.isRegularFile(resolvedBaselineJar)) {
				check(resolvedBaselineJar, maybeJar.get());
			} else {
				throw new MojoExecutionException("Invalid baseline JAR " + resolvedBaselineJar);
			}
		} else {
			throw new MojoExecutionException("No baseline version specified.");
		}
	}

	private void check(Path oldJar, Path newJar) throws MojoExecutionException {
		// Load configuration (YAML + Maven parameters)
		RoseauOptions options = loadConfiguration(oldJar, newJar);

		// Build libraries with configuration
		Library oldLibrary = options.v1().mergeWith(options.common()).toLibrary();
		Library newLibrary = options.v2().mergeWith(options.common()).toLibrary();

		// Debug
		getLog().debug("v1 classpath is: " + oldLibrary.getClasspath());
		getLog().debug("v2 classpath is: " + newLibrary.getClasspath());

		// Run diff
		RoseauReport report = Roseau.diff(oldLibrary, newLibrary);

		// Export APIs if configured
		exportApis(report);

		// Filter report based on configuration
		RoseauReport filteredReport = report.filterReport(options.diff());

		// Write reports to files if configured
		writeReports(filteredReport, options.reports());

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

	private Optional<Path> resolveBaselineDependency() {
		try {
			Artifact artifact = new DefaultArtifact(toCoordinates(baselineDependency));
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

	private static Dependency parseBaselineCoordinates(String coordinates) throws MojoExecutionException {
		String[] parts = coordinates.split(":");
		if (parts.length < 3 || parts.length > 5) {
			throw new MojoExecutionException(
				"Invalid baseline coordinates '" + coordinates +
					"'; expected groupId:artifactId:version[:extension[:classifier]].");
		}

		Dependency dep = new Dependency();
		dep.setGroupId(parts[0]);
		dep.setArtifactId(parts[1]);

		switch (parts.length) {
			case 3 -> dep.setVersion(parts[2]);
			case 4 -> {
				dep.setType(parts[2]);
				dep.setVersion(parts[3]);
			}
			case 5 -> {
				dep.setType(parts[2]);
				dep.setClassifier(parts[3]);
				dep.setVersion(parts[4]);
			}
		}

		if (isBlank(dep.getGroupId()) || isBlank(dep.getArtifactId()) || isBlank(dep.getVersion())) {
			throw new MojoExecutionException(
				"Invalid baseline coordinates '" + coordinates +
					"'; groupId, artifactId, and version must not be blank.");
		}

		return dep;
	}

	private static String toCoordinates(Dependency dep) {
		String type = dep.getType();
		String classifier = dep.getClassifier();
		boolean hasType = !isBlank(type);
		boolean hasClassifier = !isBlank(classifier);

		if (hasType && hasClassifier) {
			return "%s:%s:%s:%s:%s".formatted(dep.getGroupId(), dep.getArtifactId(), type, classifier, dep.getVersion());
		} else if (hasType) {
			return "%s:%s:%s:%s".formatted(dep.getGroupId(), dep.getArtifactId(), type, dep.getVersion());
		} else {
			return "%s:%s:%s".formatted(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
		}
	}

	private boolean isBaselineDependencyConfigured() {
		return baselineDependency != null && baselineDependency.getArtifactId() != null;
	}

	private boolean isBaselineDependencyValid() {
		return baselineDependency != null
			&& !isBlank(baselineDependency.getGroupId())
			&& !isBlank(baselineDependency.getArtifactId())
			&& !isBlank(baselineDependency.getVersion());
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private Path resolvePath(Path path) {
		if (path == null || path.isAbsolute()) {
			return path;
		}
		return project.getBasedir().toPath().resolve(path);
	}

	private List<Path> resolvePaths(List<Path> paths) {
		if (paths == null || paths.isEmpty()) {
			return List.of();
		}
		return paths.stream()
			.map(this::resolvePath)
			.toList();
	}

	private RoseauOptions normalizePaths(RoseauOptions options) {
		RoseauOptions.Classpath commonClasspath = new RoseauOptions.Classpath(
			resolvePath(options.common().classpath().pom()),
			resolvePaths(options.common().classpath().jars())
		);
		RoseauOptions.Common common = new RoseauOptions.Common(commonClasspath, options.common().excludes());

		RoseauOptions.Classpath v1Classpath = new RoseauOptions.Classpath(
			resolvePath(options.v1().classpath().pom()),
			resolvePaths(options.v1().classpath().jars())
		);
		RoseauOptions.Library v1 = new RoseauOptions.Library(
			resolvePath(options.v1().location()),
			v1Classpath,
			options.v1().excludes(),
			resolvePath(options.v1().apiReport())
		);

		RoseauOptions.Classpath v2Classpath = new RoseauOptions.Classpath(
			resolvePath(options.v2().classpath().pom()),
			resolvePaths(options.v2().classpath().jars())
		);
		RoseauOptions.Library v2 = new RoseauOptions.Library(
			resolvePath(options.v2().location()),
			v2Classpath,
			options.v2().excludes(),
			resolvePath(options.v2().apiReport())
		);

		RoseauOptions.Diff diff = new RoseauOptions.Diff(
			resolvePath(options.diff().ignore()),
			options.diff().sourceOnly(),
			options.diff().binaryOnly()
		);

		return new RoseauOptions(common, v1, v2, diff, options.reports());
	}

	private Path resolveReportPath(Path reportPath) {
		Path moduleReportDirectory = resolvePath(reportDirectory.toPath());
		if (reportPath.isAbsolute()) {
			return reportPath;
		}
		if (moduleReportDirectory != null) {
			return moduleReportDirectory.resolve(reportPath);
		}
		return resolvePath(reportPath);
	}

	/**
	 * Configuration for report generation.
	 */
	public static class ReportConfig {
		/**
		 * Report file path. Relative paths are resolved against {@code reportDirectory}.
		 */
		@Parameter(required = true)
		private String file;
		/**
		 * Report format (CSV, HTML, JSON, MD).
		 */
		@Parameter(required = true)
		private String format;

		/**
		 * Converts this Maven configuration to a Roseau Report.
		 *
		 * @return the Roseau Report instance
		 */
		RoseauOptions.Report toRoseauReport() {
			if (file == null || file.isBlank() || format == null || format.isBlank()) {
				return null;
			}
			return new RoseauOptions.Report(Path.of(file),
				BreakingChangesFormatterFactory.valueOf(format.toUpperCase(Locale.ROOT)));
		}
	}

	private static void makeParent(Path path) throws IOException {
		Path parentDir = path.getParent();
		if (parentDir != null && !Files.exists(parentDir)) {
			Files.createDirectories(parentDir);
		}
	}
}
