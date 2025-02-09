package com.github.maracas.roseau.smoke;

import com.github.maracas.roseau.api.extractors.jar.JarAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.spoon.SpoonAPIExtractor;
import com.github.maracas.roseau.spoon.SpoonUtils;
import com.google.common.base.Stopwatch;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiClass;
import jdk.javadoc.doclet.Reporter;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.revapi.AnalysisContext;
import org.revapi.Revapi;
import org.revapi.base.FileArchive;
import org.revapi.java.JavaApiAnalyzer;
import spoon.reflect.CtModel;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class BenchmarkIT {

	static Stream<String> libraries() {
		return Stream.of(
			"com.google.dagger:dagger:2.55",
			"ch.qos.logback:logback-core:1.5.16",
			//"ch.qos.logback:logback-classic:1.5.16",
			"org.apache.logging.log4j:log4j-core:2.24.3",
			"org.apache.logging.log4j:log4j-api:2.24.3",
			"org.slf4j:slf4j-simple:2.0.16",
			"org.slf4j:slf4j-api:2.0.16",
			"com.fasterxml.jackson.core:jackson-core:2.18.2",
			"org.apache.httpcomponents.client5:httpclient5:5.4.2",
			"fr.inria.gforge.spoon:spoon-core:11.2.0",
			"commons-logging:commons-logging:1.3.5",
			"org.springframework:spring-web:6.2.2",
			"com.h2database:h2:2.3.232",
			"org.hamcrest:hamcrest:3.0",
			"org.springframework:spring-beans:6.2.2",
			"org.osgi:org.osgi.core:6.0.0",
			"com.alibaba:fastjson:2.0.54",
			"commons-collections:commons-collections:3.2.2",
			"org.json:json:20250107",
			"commons-beanutils:commons-beanutils:1.10.0",
			"com.squareup.retrofit2:retrofit:2.11.0",
			"org.assertj:assertj-core:3.27.3",
			"commons-codec:commons-codec:1.18.0",
			"com.google.guava:guava:32.1.3-jre",
			"org.apache.commons:commons-lang3:3.17.0",
			"commons-io:commons-io:2.18.0",
			"org.eclipse.collections:eclipse-collections-api:11.1.0",
			"org.springframework:spring-core:6.1.5",
			"io.dropwizard:dropwizard-core:4.0.1",
			"io.projectreactor:reactor-core:3.6.3",
			"org.reactivestreams:reactive-streams:1.0.4",
			"org.apache.kafka:kafka-clients:3.6.0",
			"com.google.code.gson:gson:2.10.1",
			"org.junit.jupiter:junit-jupiter-api:5.10.1",
			"com.squareup:javapoet:1.13.0",
			"org.jooq:joor-java-8:0.9.15",
			"joda-time:joda-time:2.12.5",
			"com.google.auto.service:auto-service:1.1.1"
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("libraries")
	@Timeout(value = 2, unit = TimeUnit.MINUTES)
	void analyzeLibrary(String libraryGAV) {
		String[] parts = libraryGAV.split(":");
		String groupId = parts[0];
		String artifactId = parts[1];
		String version = parts[2];

		try {
			Path binaryJar = downloadBinaryJar(groupId, artifactId, version);
			Path sourcesJar = downloadSourcesJar(groupId, artifactId, version);
			Path sourcesDir = extractSourcesJar(sourcesJar);

			long iloc = 0;
			long itypes = 0;
			long imethods = 0;
			long ifields = 0;
			long iparsing = 0;
			long iapi = 0;
			long ijarapi = 0;
			long idiff = 0;
			long japiTime = 0;
			long roseauBC = 0;
			long japiBC = 0;
			long revapiTime = 0;
			long revapiBC = 0;
			for (int i = 0; i < 10; i++) {
				// Parse, build API, (self-)diff
				Stopwatch sw = Stopwatch.createStarted();
				JarAPIExtractor jarExtractor = new JarAPIExtractor();
				API jarApi = jarExtractor.extractAPI(binaryJar);
				long jarApiTime = sw.elapsed().toMillis();
				sw.reset();
				sw.start();

				CtModel model = SpoonUtils.buildModel(sourcesDir, Duration.ofMinutes(1));
				long parsingTime = sw.elapsed().toMillis();
				sw.reset();
				sw.start();

				SpoonAPIExtractor sourcesExtractor = new SpoonAPIExtractor();
				API sourcesApi = sourcesExtractor.extractAPI(model);
				long sourcesApiTime = sw.elapsed().toMillis();
				sw.reset();
				sw.start();

//				System.out.println("### JAR VS SOURCES:");
//				com.github.maracas.roseau.smoke.APIDiff.diffAPI(jarApi, sourcesApi);
//				System.out.println("### SOURCES VS JAR:");
//				com.github.maracas.roseau.smoke.APIDiff.diffAPI(sourcesApi, jarApi);

//				APIDiff jarToSourcesDiff = new APIDiff(jarApi, sourcesApi);
//				List<BreakingChange> jarToSourcesBCs = jarToSourcesDiff.diff();
//				APIDiff sourcesToJarDiff = new APIDiff(sourcesApi, jarApi);
//				List<BreakingChange> sourcesToJarBCs = sourcesToJarDiff.diff();
//				long diffTime = sw.elapsed().toMillis();

				APIDiff sourcesToSourcesDiff = new APIDiff(sourcesApi, sourcesApi);
				List<BreakingChange> sourcesToSourcesBCs = sourcesToSourcesDiff.diff();
				long diffTime = sw.elapsed().toMillis();
				sw.reset();
				sw.start();

				// Stats
				long loc = countLinesOfCode(sourcesDir);
				long numTypes = sourcesApi.getAllTypes().count();
				int numMethods = sourcesApi.getAllTypes()
					.mapToInt(type -> type.getDeclaredMethods().size())
					.sum();
				int numFields = sourcesApi.getAllTypes()
					.mapToInt(type -> type.getDeclaredFields().size())
					.sum();

//				System.out.printf("Processed %s (%d LoC, %d types, %d methods, %d fields)%n" +
//						"\tParsing: %dms API: %sms Diff: %dms%n" +
//						"\tJAR API: %dms%n" +
//						"\tJAR to Sources BCs: %d%n" +
//						"\tSources to JAR BCs: %d%n",
//					libraryGAV, loc, numTypes, numMethods, numFields, parsingTime, sourcesApiTime, diffTime, jarApiTime,
//					jarToSourcesBCs.size(), sourcesToJarBCs.size());

//				System.out.println("JAR to Sources BCs:");
//				System.out.println(jarToSourcesBCs.stream()
//					.map(BreakingChange::toString).collect(Collectors.joining("\n")));
//				System.out.println("Sources to JAR BCs:");
//				System.out.println(sourcesToJarBCs.stream()
//					.map(BreakingChange::toString).collect(Collectors.joining("\n")));

//				cleanup(sourcesJar);
//				cleanup(binaryJar);
//				cleanup(sourcesDir);

				sw.reset();
				sw.start();
				Options opts = Options.newDefault();
				opts.setAccessModifier(AccessModifier.PACKAGE_PROTECTED);
				opts.setOutputOnlyModifications(true);
				opts.setIgnoreMissingClasses(true);
				opts.setIncludeSynthetic(true);
				var comparatorOptions = JarArchiveComparatorOptions.of(opts);
				var jarArchiveComparator = new JarArchiveComparator(comparatorOptions);
				var v1Archive = new JApiCmpArchive(binaryJar.toFile(), "1.0.0");
				var v2Archive = new JApiCmpArchive(binaryJar.toFile(), "2.0.0");
				List<JApiClass> jApiClasses = jarArchiveComparator.compare(v1Archive, v2Archive);
				japiTime = sw.elapsed().toMillis();

				sw.reset();
				sw.start();
				var revapi = Revapi.builder()
					.withAnalyzers(JavaApiAnalyzer.class)
					.withReporters(SilentReporter.class)
					.build();

				var v1ArchiveRev = new FileArchive(binaryJar.toFile());
				var v1Api = org.revapi.API.of(v1ArchiveRev).build();
				var v2ArchiveRev = new FileArchive(binaryJar.toFile());
				var v2Api = org.revapi.API.of(v2ArchiveRev).build();

				var analysisContext = AnalysisContext.builder()
					.withOldAPI(v1Api)
					.withNewAPI(v2Api)
					.build();

				long revapicount = 0;
				try (var results = revapi.analyze(analysisContext)) {
					for (var entry : results.getExtensions().getReporters().entrySet()) {
						var reporter = entry.getKey();

						if (reporter.getInstance() instanceof SilentReporter silentReporter) {
							revapicount += silentReporter.getReportCount();
						}
					}
				} catch (Exception ignored) {}
				revapiTime = sw.elapsed().toMillis();

				iloc = loc;
				itypes = numTypes;
				imethods = numMethods;
				ifields = numFields;
				iparsing = parsingTime;
				iapi = sourcesApiTime;
				ijarapi = jarApiTime;
				idiff = diffTime;
				roseauBC = sourcesToSourcesBCs.size();
				japiBC = jApiClasses.size();
				revapiBC = revapicount;

				// Check everything went well
//				assertFalse(sourcesApi.getAllTypes().findAny().isEmpty());
//				assertTrue(jarToSourcesBCs.isEmpty());
//				assertTrue(sourcesToJarBCs.isEmpty());
			}
			System.out.printf("CSV=%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
				libraryGAV, iloc, itypes, imethods, ifields, iparsing, iapi, ijarapi, idiff,
					iparsing+iapi+idiff, ijarapi+idiff, japiTime, revapiTime, roseauBC, japiBC, revapiBC);
		} catch (Exception e) {
			fail("Failed to process " + libraryGAV, e);
		}
	}

	final class SilentReporter implements Reporter {
		private static final String CONFIG_ROOT_PATH = "revapi.reporter.roseau.silent-reporter";

		private SortedSet<Report> reports;

		public boolean hasReports() {
			return !reports.isEmpty();
		}

		public int getReportCount() {
			return reports.size();
		}

		@Override
		public void initialize(@Nonnull AnalysisContext analysis) {
			this.reports = new TreeSet<>(getReportsByElementOrderComparator());
		}

		@Override
		public void report(Report report) {
			if (!report.getDifferences().isEmpty()) {
				reports.add(report);
			}
		}

		@Override
		public String getExtensionId() { return CONFIG_ROOT_PATH; }

		@Nullable
		@Override
		public Reader getJSONSchema() { return null; }

		@Override
		public void close() throws Exception {}

		private static Comparator<Report> getReportsByElementOrderComparator() {
			return (new ReportComparator.Builder()).withComparisonStrategy(ReportComparator.Strategy.HIERARCHICAL).build();
		}
	}

	private Path downloadSourcesJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-sources.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private Path downloadBinaryJar(String groupId, String artifactId, String version) throws IOException, InterruptedException {
		return download(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
			groupId.replace('.', '/'), artifactId, version, artifactId, version));
	}

	private Path download(String url) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.build();

		Path tempFile = Files.createTempFile("sources-", ".jar");
		HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));

		if (response.statusCode() != 200) {
			Files.deleteIfExists(tempFile);
			throw new IOException("Failed to download JAR: HTTP " + response.statusCode());
		}

		return tempFile;
	}

	private Path extractSourcesJar(Path jarPath) throws IOException {
		Path outputDir = Files.createTempDirectory("sources-");
		try (JarFile jar = new JarFile(jarPath.toFile())) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				Path entryPath = outputDir.resolve(entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					Files.createDirectories(entryPath.getParent());
					try (InputStream is = jar.getInputStream(entry)) {
						Files.copy(is, entryPath);
					}
				}
			}
		}
		return outputDir;
	}

	private long countLinesOfCode(Path sourcesDir) throws IOException {
		return Files.walk(sourcesDir)
			.filter(path -> path.toString().endsWith(".java"))
			.mapToLong(path -> {
				try (BufferedReader reader = Files.newBufferedReader(path)) {
					return reader.lines().count();
				} catch (IOException e) {
					System.err.println("Failed to count lines in " + path + ": " + e.getMessage());
					return 0;
				}
			})
			.sum();
	}

	private void cleanup(Path path) throws IOException {
		if (path.toFile().isDirectory())
			deleteDirectory(path);
		else
			Files.deleteIfExists(path);
	}

	private void deleteDirectory(Path path) throws IOException {
		if (Files.exists(path)) {
			Files.walk(path)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.delete(p);
					} catch (IOException e) {
						System.err.println("Failed to delete " + p + ": " + e.getMessage());
					}
				});
		}
	}
}

