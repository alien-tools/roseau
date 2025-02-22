package com.github.maracas.roseau.extractors.jdt;

import com.github.maracas.roseau.extractors.TimestampChangedFilesProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.*;

class IncrementalJdtAPIExtractorTest {
	@Test
	void old_references_are_reset(@TempDir Path wd) throws Exception {
		var provider = new TimestampChangedFilesProvider(wd);

		Files.writeString(wd.resolve("I.java"), "public interface I {}");
		Files.writeString(wd.resolve("C.java"), "public class C implements I {}");
		Files.writeString(wd.resolve("R.java"), "public record R() {}");

		var extractor = new JdtAPIExtractor();
		var api1 = extractor.extractAPI(wd);

		provider.refresh(api1, Instant.now().toEpochMilli());
		Thread.sleep(10); // Legit need it ;)
		Files.writeString(wd.resolve("I.java"), "public interface I { void m(); }");
		var changedFiles = provider.getChangedFiles();

		var incrementalExtractor = new IncrementalJdtAPIExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1);

		var i1 = assertInterface(api1, "I");
		var i2 = assertInterface(api2, "I");
		var c1 = assertClass(api1, "C");
		var c2 = assertClass(api2, "C");
		var r1 = assertRecord(api1, "R");
		var r2 = assertRecord(api2, "R");

		assertThat(i1, is(not(equalTo(i2))));
		assertThat(c1, is(equalTo(c2)));
		assertThat(c1, is(not(sameInstance(c2))));
		assertThat(r1, is(equalTo(r2)));
		assertThat(r1, is(not(sameInstance(r2))));

		assertThat(c1.getImplementedInterfaces().getFirst().getResolvedApiType().get(), is(sameInstance(i1)));
		assertThat(c2.getImplementedInterfaces().getFirst().getResolvedApiType().get(), is(sameInstance(i2)));

		assertFalse(c1.findMethod("m()").isPresent());
		assertTrue(c2.findMethod("m()").isPresent());
	}
}
