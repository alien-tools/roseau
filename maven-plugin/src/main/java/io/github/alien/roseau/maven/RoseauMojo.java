package io.github.alien.roseau.maven;

import io.github.alien.roseau.JavaRelease;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.options.RoseauOptions;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final String COMPILER_PLUGIN = "org.apache.maven.plugins:maven-compiler-plugin";
	// A javac -source/-target/--release value: N, or 1.N for N <= 8
	private static final Pattern COMPILER_VERSION = Pattern.compile("(?:1\\.)?(\\d{1,3})");

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
	 * Fails the build when some types cannot be resolved, instead of reporting breaking changes computed from an
	 * incomplete API model. Recommended in CI, where the warnings Roseau logs for unresolved types go unnoticed.
	 */
	@Parameter(property = "roseau.failOnUnresolvedTypes")
	private Boolean failOnUnresolvedTypes;

	/**
	 * Ignores the module declarations of the compared artifacts and considers every {@code public} type as part of
	 * the API, matching the view of clients using the library from the class path.
	 */
	@Parameter(property = "roseau.ignoreModule")
	private Boolean ignoreModule;

	/**
	 * The Java release on which to check compatibility (e.g., {@code 8}, {@code 17}, {@code 21}; {@code 1.8} means
	 * {@code 8}). Both versions are compared on this release. Defaults to the release the project compiles for: the
	 * compiler plug-in's {@code release} (or {@code maven.compiler.release}), else its {@code target} (or
	 * {@code maven.compiler.target}); without either, to the release of the JDK running Maven.
	 */
	@Parameter(property = "roseau.javaRelease")
	private String javaRelease;

	@Parameter(property = "maven.compiler.release", readonly = true)
	private String compilerRelease;

	@Parameter(property = "maven.compiler.target", readonly = true)
	private String compilerTarget;

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
	 * Exports API models to JSON files.
	 *
	 * @param report  the RoseauReport containing the APIs
	 * @param options the merged export configuration
	 * @throws MojoExecutionException if an error occurs while exporting APIs
	 */
	private void exportApis(RoseauReport report, RoseauOptions options) throws MojoExecutionException {
		if (options.v1().apiReport() != null) {
			exportApi(report.v1(), options.v1().apiReport());
		}

		if (options.v2().apiReport() != null) {
			exportApi(report.v2(), options.v2().apiReport());
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
	private void writeReports(RoseauReport report, List<RoseauOptions.Report> reportConfigs)
		throws MojoExecutionException {
		if (reportConfigs == null || reportConfigs.isEmpty()) {
			return;
		}

		for (RoseauOptions.Report config : reportConfigs) {
			Path outputPath = resolveReportPath(config.file());
			try {
				makeParent(outputPath);
				report.writeReport(config.format(), outputPath);
				getLog().info(String.format("%s report written to %s", config.format(), outputPath));
			} catch (RoseauException | IOException e) {
				throw new MojoExecutionException("Failed to write %s report to %s".formatted(
					config.format(), outputPath), e);
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
		RoseauOptions options = RoseauOptions.newDefault().mergeWith(buildDefaults());
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
		RoseauOptions mavenOptions = buildMavenOptions(oldJar, newJar, parseJavaRelease());
		options = options.mergeWith(mavenOptions);
		options = normalizePaths(options);
		if (Boolean.TRUE.equals(options.diff().sourceOnly()) && Boolean.TRUE.equals(options.diff().binaryOnly())) {
			throw new MojoExecutionException("sourceOnly and binaryOnly cannot both be true.");
		}

		getLog().debug("Roseau options = " + options);
		return options;
	}

	/**
	 * Builds RoseauOptions from Maven configuration parameters.
	 *
	 * @param oldJar              the path to the baseline JAR
	 * @param newJar              the path to the current JAR
	 * @param explicitJavaRelease the {@code javaRelease} parameter, if set
	 * @return the RoseauOptions instance
	 */
	private RoseauOptions buildMavenOptions(Path oldJar, Path newJar, Integer explicitJavaRelease) {
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
		RoseauOptions.Common common = new RoseauOptions.Common(commonClasspath, commonExclude, ignoreModule,
			explicitJavaRelease);

		// Build Library v1 (baseline)
		RoseauOptions.Classpath v1Classpath = new RoseauOptions.Classpath(
			resolvePath(baselineClasspathPom), baselineClasspathResolved);
		RoseauOptions.Library v1 = new RoseauOptions.Library(
			oldJar, v1Classpath, new RoseauOptions.Exclude(List.of(), List.of()), resolvePath(exportBaselineApi), null);

		// Build Library v2 (current)
		RoseauOptions.Classpath v2Classpath = new RoseauOptions.Classpath(null, List.of());
		RoseauOptions.Library v2 = new RoseauOptions.Library(
			newJar, v2Classpath, new RoseauOptions.Exclude(List.of(), List.of()), resolvePath(exportCurrentApi), null);

		Boolean mavenSourceOnly = sourceOnly;
		Boolean mavenBinaryOnly = binaryOnly;
		if (Boolean.TRUE.equals(sourceOnly) && binaryOnly == null) {
			mavenBinaryOnly = false;
		} else if (Boolean.TRUE.equals(binaryOnly) && sourceOnly == null) {
			mavenSourceOnly = false;
		}
		RoseauOptions.Diff diff = new RoseauOptions.Diff(null, mavenSourceOnly, mavenBinaryOnly, failOnUnresolvedTypes);

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
	 * Resolves the project's compile classpath, including provided and system dependencies.
	 *
	 * @return a list of paths to the dependency JARs or class directories
	 */
	private List<Path> resolveProjectClasspath() {
		return project.getArtifacts().stream()
			.filter(artifact -> "compile".equals(artifact.getScope()) || "provided".equals(artifact.getScope()) ||
				"system".equals(artifact.getScope()))
			.map(org.apache.maven.artifact.Artifact::getFile)
			.filter(RoseauMojo::isClasspathEntry)
			.map(File::toPath)
			.toList();
	}

	/**
	 * Checks whether the given file is a usable classpath entry: a JAR, or the class directory that Maven attaches to
	 * reactor dependencies that haven't been packaged yet.
	 *
	 * @param file the file to check, possibly {@code null}
	 * @return whether the file can be used as a classpath entry
	 */
	private static boolean isClasspathEntry(File file) {
		return file != null && (Files.isRegularFile(file.toPath()) || Files.isDirectory(file.toPath()));
	}

	/**
	 * Resolves the baseline artifact and its transitive dependencies.
	 *
	 * @return a list of paths to the baseline dependency JARs or class directories, or an empty list if
	 * resolution fails
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
				.filter(RoseauMojo::isClasspathEntry)
				.map(File::toPath)
				.toList();
		} catch (DependencyResolutionException e) {
			getLog().warn("Could not resolve baseline dependencies: " + e.getMessage());
			return List.of();
		}
	}

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

		// Build libraries with configuration and run diff
		RoseauReport report;
		try {
			Library oldLibrary = options.v1().toLibrary(options.common());
			Library newLibrary = options.v2().toLibrary(options.common());
			getLog().debug("v1 classpath is: " + oldLibrary.getClasspath());
			getLog().debug("v2 classpath is: " + newLibrary.getClasspath());
			report = Roseau.diff(oldLibrary, newLibrary);
		} catch (RoseauException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		// Reject a verdict computed from an incomplete API model before reporting anything
		if (Boolean.TRUE.equals(options.diff().failOnUnresolved())) {
			try {
				report.checkFullyResolved();
			} catch (RoseauException e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
		}

		// Export APIs if configured
		exportApis(report, options);

		// Filter report based on configuration
		RoseauReport filteredReport = report.filterReport(options.diff());

		// Write reports to files if configured
		writeReports(filteredReport, options.reports());

		// Get breaking changes for display and fail checks
		List<BreakingChange> bcs = filteredReport.getBreakingChanges();

		if (bcs.isEmpty()) {
			getLog().info("No breaking changes found on Java " + filteredReport.getJavaRelease() + ".");
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

	private Integer parseJavaRelease() throws MojoExecutionException {
		if (javaRelease == null || javaRelease.isBlank()) {
			return null;
		}
		String value = javaRelease.trim();
		try {
			return "1.8".equals(value) ? 8 : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new MojoExecutionException("Invalid javaRelease parameter: expected an integer release, got '" +
				javaRelease + "'", e);
		}
	}

	/**
	 * The release the project compiles for, as maven-compiler-plugin resolves it: its {@code release} (configured,
	 * else {@code maven.compiler.release}), else its {@code target} (configured, else {@code maven.compiler.target}).
	 */
	private RoseauOptions buildDefaults() {
		Optional<CompilerSetting> setting = compilerSetting("release", "maven.compiler.release", compilerRelease)
			.or(() -> compilerSetting("target", "maven.compiler.target", compilerTarget));
		Integer release = setting.flatMap(this::toSupportedRelease).orElse(null);
		RoseauOptions.Common common = new RoseauOptions.Common(null, null, null, release);
		return new RoseauOptions(common, null, null, null, null);
	}

	private record CompilerSetting(String source, String value) {
	}

	private Optional<CompilerSetting> compilerSetting(String parameter, String property, String propertyValue) {
		Plugin compiler = project.getPlugin(COMPILER_PLUGIN);
		if (compiler != null) {
			Optional<String> configured = compiler.getExecutions().stream()
				.filter(execution -> "default-compile".equals(execution.getId()))
				.findFirst()
				.flatMap(execution -> configurationValue(execution.getConfiguration(), parameter))
				.or(() -> configurationValue(compiler.getConfiguration(), parameter));
			if (configured.isPresent()) {
				return Optional.of(new CompilerSetting("maven-compiler-plugin's " + parameter, configured.get()));
			}
		}

		return Optional.ofNullable(propertyValue)
			.filter(value -> !value.isBlank())
			.map(value -> new CompilerSetting(property, value));
	}

	private static Optional<String> configurationValue(Object configuration, String parameter) {
		if (configuration instanceof Xpp3Dom dom && dom.getChild(parameter) != null) {
			return Optional.ofNullable(dom.getChild(parameter).getValue())
				.map(String::trim)
				.filter(value -> !value.isEmpty() && !value.startsWith("${"));
		}
		return Optional.empty();
	}

	private Optional<Integer> toSupportedRelease(CompilerSetting setting) {
		Matcher matcher = COMPILER_VERSION.matcher(setting.value());
		if (!matcher.matches()) {
			getLog().warn("Ignoring %s '%s', which is not a Java release".formatted(setting.source(), setting.value()));
			return Optional.empty();
		}

		int release = Integer.parseInt(matcher.group(1));
		if (release < JavaRelease.MINIMUM) {
			getLog().warn("The project compiles for Java %d (%s), older than Roseau supports; checking compatibility on Java %d"
				.formatted(release, setting.source(), JavaRelease.MINIMUM));
			return Optional.of(JavaRelease.MINIMUM);
		}
		if (release > JavaRelease.host()) {
			getLog().warn(("The project compiles for Java %d (%s), newer than the JDK running Maven; " +
				"checking compatibility on Java %d").formatted(release, setting.source(), JavaRelease.host()));
			return Optional.empty();
		}

		getLog().debug("Java release %d from %s".formatted(release, setting.source()));
		return Optional.of(release);
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
			getLog().warn("Could not resolve baseline artifact " + toCoordinates(baselineDependency) + ": "
				+ e.getMessage());
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
		RoseauOptions.Common common = new RoseauOptions.Common(commonClasspath, options.common().excludes(),
			options.common().ignoreModule(), options.common().javaRelease());

		RoseauOptions.Classpath v1Classpath = new RoseauOptions.Classpath(
			resolvePath(options.v1().classpath().pom()),
			resolvePaths(options.v1().classpath().jars())
		);
		RoseauOptions.Library v1 = new RoseauOptions.Library(
			resolvePath(options.v1().location()),
			v1Classpath,
			options.v1().excludes(),
			resolvePath(options.v1().apiReport()),
			options.v1().ignoreModule()
		);

		RoseauOptions.Classpath v2Classpath = new RoseauOptions.Classpath(
			resolvePath(options.v2().classpath().pom()),
			resolvePaths(options.v2().classpath().jars())
		);
		RoseauOptions.Library v2 = new RoseauOptions.Library(
			resolvePath(options.v2().location()),
			v2Classpath,
			options.v2().excludes(),
			resolvePath(options.v2().apiReport()),
			options.v2().ignoreModule()
		);

		RoseauOptions.Diff diff = new RoseauOptions.Diff(
			resolvePath(options.diff().ignore()),
			options.diff().sourceOnly(),
			options.diff().binaryOnly(),
			options.diff().failOnUnresolved()
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
