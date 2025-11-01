package io.github.alien.roseau.smoke;

import com.google.common.base.Stopwatch;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.extractors.ExtractorType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Disabled("Not sure how to share/download JDK's sources and binaries; set JDK_BIN_PATH & JDK_SRC_PATH")
class JdkTestIT {
	static final String JDK_BIN_PATH = "/data/jdk21/bin";
	static final String JDK_SRC_PATH = "/data/jdk21/sources";

	static Stream<Path> jmods() {
		try {
			return Files.list(Path.of(JDK_BIN_PATH, "jmods"));
		} catch (IOException e) {
			throw new RuntimeException("Couldn't list jmods");
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("jmods")
	@Timeout(value = 1, unit = TimeUnit.MINUTES)
	void jdk21Spoon(Path jmod) {
		var moduleName = jmod.getFileName().toString().replace(".jmod", "");
		var src = Path.of(String.format("%s/src/%s/share/classes", JDK_SRC_PATH, moduleName));

		if (!src.toFile().exists())
			fail("No sources for " + jmod);

		var sw = Stopwatch.createUnstarted();
		var classpath = jmods().filter(mod -> !mod.equals(jmod)).toList();
		var srcLibrary = Library.builder()
			.location(src)
			.classpath(classpath)
			.extractorType(ExtractorType.SPOON)
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
	@Timeout(value = 1, unit = TimeUnit.MINUTES)
	void jdk21Jdt(Path jmod) {
		var moduleName = jmod.getFileName().toString().replace(".jmod", "");
		var src = Path.of(String.format("%s/src/%s/share/classes", JDK_SRC_PATH, moduleName));

		if (!src.toFile().exists())
			fail("No sources for " + jmod);

		var sw = Stopwatch.createUnstarted();
		var classpath = jmods().filter(mod -> !mod.equals(jmod)).toList();
		var srcLibrary = Library.builder()
			.location(src)
			.classpath(classpath)
			.extractorType(ExtractorType.JDT)
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
	@Timeout(value = 1, unit = TimeUnit.MINUTES)
	void jdk21Asm(Path jmod) {
		var sw = Stopwatch.createUnstarted();
		var classpath = jmods().filter(mod -> !mod.equals(jmod)).toList();
		var jarLibrary = Library.builder()
			.location(jmod)
			.classpath(classpath)
			.extractorType(ExtractorType.ASM)
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
}
