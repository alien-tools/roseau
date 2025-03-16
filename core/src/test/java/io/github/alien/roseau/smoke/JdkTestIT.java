package io.github.alien.roseau.smoke;

import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.extractors.asm.AsmAPIExtractor;
import io.github.alien.roseau.extractors.jdt.JdtAPIExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonAPIExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonUtils;
import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.jsoup.helper.Validate.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Not sure how to share/download JDK's sources and binaries; set JDK_BIN_PATH & JDK_SRC_PATH")
class JdkTestIT {
	static final String JDK_BIN_PATH = "jdk21/bin";
	static final String JDK_SRC_PATH = "jdk21/sources";

	static Stream<Path> jmods() throws IOException {
		return Files.list(Path.of(JDK_BIN_PATH, "jmods"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("jmods")
	@Timeout(value = 1, unit = TimeUnit.MINUTES)
	void jdk21(Path jmod) {
		var moduleName = jmod.getFileName().toString().replace(".jmod", "");
		var src = Path.of(String.format("%s/src/%s/share/classes", JDK_SRC_PATH, moduleName));
		var sw = Stopwatch.createUnstarted();
		var spoonExtractor = new SpoonAPIExtractor();
		var asmExtractor = new AsmAPIExtractor();
		var jdtExtractor = new JdtAPIExtractor();

		if (!src.toFile().exists())
			fail("No sources for " + jmod);

		sw.reset().start();
		var jarApi = asmExtractor.extractAPI(jmod);
		var jarApiTime = sw.elapsed().toMillis();
		System.out.printf("ASM API took %dms (%d types)%n", jarApiTime, jarApi.getAllTypes().count());

		sw.reset().start();
		var jdtApi = jdtExtractor.extractAPI(src);
		var jdtApiTime = sw.elapsed().toMillis();
		System.out.printf("JDT API took %dms (%d types)%n", jdtApiTime, jdtApi.getAllTypes().count());

		sw.reset().start();
		var model = SpoonUtils.buildModel(src, Duration.ofMinutes(1));
		var parsingTime = sw.elapsed().toMillis();
		System.out.printf("Parsing took %dms%n", parsingTime);

		sw.reset().start();
		var srcApi = spoonExtractor.extractAPI(model);
		var apiTime = sw.elapsed().toMillis();
		System.out.printf("Spoon API took %dms (%d types)%n", apiTime, srcApi.getAllTypes().count());

		sw.reset().start();
		var bcs = new APIDiff(srcApi, srcApi).diff();
		var diffTime = sw.elapsed().toMillis();
		System.out.printf("Diff took %dms (%d BCs)%n", diffTime, bcs.size());

		//System.out.println("JAR to Sources API diff:");
		//diffAPIs(jarApi, srcApi);
		//System.out.println("Sources to JAR API diff:");
		//diffAPIs(srcApi, jarApi);

		assertTrue(bcs.isEmpty());
	}
}
