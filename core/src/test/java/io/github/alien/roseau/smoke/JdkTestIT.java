package io.github.alien.roseau.smoke;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("smoke")
class JdkTestIT {
	static final Path JDK_HOME = Path.of(System.getProperty("java.home"));
	static final Path JMODS_DIR = JDK_HOME.resolve("jmods");
	static final Path SRC_ZIP = JDK_HOME.resolve("lib").resolve("src.zip");
	static final Path EXTRACTED_SOURCES_DIR = Path.of("target", "jdk-sources", System.getProperty("java.version"));
	static final String JDT_LOGGER = "io.github.alien.roseau.extractors.jdt.JdtTypesExtractor";

	static Stream<Path> jmods() {
		try {
			return Files.list(JMODS_DIR)
				.filter(path -> path.getFileName().toString().endsWith(".jmod"))
				.sorted();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't list jmods in " + JMODS_DIR, e);
		}
	}

	@BeforeAll
	static void setUpJdkSources() {
		assumeTrue(Files.isDirectory(JMODS_DIR), () -> "Missing JDK jmods directory: " + JMODS_DIR);
		assumeTrue(Files.isRegularFile(SRC_ZIP), () -> "Missing JDK source archive: " + SRC_ZIP);
		Configurator.setLevel(JDT_LOGGER, Level.ERROR);
		extractSources();
	}

	@AfterAll
	static void restoreLogging() {
		Configurator.setLevel(JDT_LOGGER, Level.WARN);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("jmods")
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	void jdkJdt(Path jmod) {
		var moduleName = jmod.getFileName().toString().replace(".jmod", "");
		assumeTrue(!moduleName.equals("java.base"),
			"JDT cannot currently parse java.base sources without conflicting with the running boot module");
		var src = EXTRACTED_SOURCES_DIR.resolve(moduleName);
		assumeTrue(Files.isDirectory(src), () -> "No sources for " + jmod);

		var sw = Stopwatch.createUnstarted();
		var srcLibrary = Library.builder()
			.location(src)
			.classpath(List.of())
			.build();

		sw.reset().start();
		var api = Roseau.buildAPI(srcLibrary);
		var apiTime = sw.elapsed().toMillis();
		System.out.printf("[%s] API took %dms (%d types, %d exported)%n", jmod.getFileName(), apiTime,
			api.getLibraryTypes().getAllTypes().size(), api.getExportedTypes().size());

		sw.reset().start();
		var report = Roseau.diff(api, api);
		var diffTime = sw.elapsed().toMillis();
		System.out.printf("[%s] Diff took %dms (%d BCs)%n", jmod.getFileName(), diffTime,
			report.getAllBreakingChanges().size());

		assertThat(report.getAllBreakingChanges()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("jmods")
	@Timeout(value = 5, unit = TimeUnit.MINUTES)
	void jdkAsm(Path jmod) {
		var sw = Stopwatch.createUnstarted();
		var classpath = jmodsExcept(jmod);
		var jarLibrary = Library.builder()
			.location(jmod)
			.classpath(classpath)
			.build();

		sw.reset().start();
		var api = Roseau.buildAPI(jarLibrary);
		var apiTime = sw.elapsed().toMillis();
		System.out.printf("[%s] API took %dms (%d types, %d exported)%n", jmod.getFileName(), apiTime,
			api.getLibraryTypes().getAllTypes().size(), api.getExportedTypes().size());

		sw.reset().start();
		var report = Roseau.diff(api, api);
		var diffTime = sw.elapsed().toMillis();
		System.out.printf("[%s] Diff took %dms (%d BCs)%n", jmod.getFileName(), diffTime, report.getAllBreakingChanges().size());

		assertThat(report.getAllBreakingChanges()).isEmpty();
	}

	private static List<Path> jmodsExcept(Path excluded) {
		try (var paths = jmods()) {
			return paths.filter(mod -> !mod.equals(excluded)).toList();
		}
	}

	private static void extractSources() {
		var readyMarker = EXTRACTED_SOURCES_DIR.resolve(".ready");
		if (Files.isRegularFile(readyMarker)) {
			return;
		}

		try {
			deleteRecursively(EXTRACTED_SOURCES_DIR);
			Files.createDirectories(EXTRACTED_SOURCES_DIR);
			try (JarFile jar = new JarFile(SRC_ZIP.toFile())) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					var entry = entries.nextElement();
					var entryPath = EXTRACTED_SOURCES_DIR.resolve(entry.getName()).normalize();
					if (!entryPath.startsWith(EXTRACTED_SOURCES_DIR)) {
						throw new IOException("Invalid source archive entry: " + entry.getName());
					}

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
			Files.writeString(readyMarker, "ok");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (!Files.exists(root)) {
			return;
		}

		try (var paths = Files.walk(root)) {
			paths.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}
}
