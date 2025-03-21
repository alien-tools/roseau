package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalJdtAPIExtractorTest {
	@Test
	void old_references_are_reset(@TempDir Path wd) throws Exception {
		var i = wd.resolve("I.java");
		Files.writeString(i, "public interface I {}");
		Files.writeString(wd.resolve("C.java"), "public class C implements I {}");
		Files.writeString(wd.resolve("R.java"), "public record R() {}");

		var extractor = new JdtAPIExtractor();
		var api1 = extractor.extractAPI(wd);

		Files.writeString(i, "public interface I { void m(); }");
		var changedFiles = new ChangedFiles(Set.of(i), Set.of(), Set.of());

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

		assertThat(c1.getImplementedInterfaces().getFirst(), is(equalTo(c2.getImplementedInterfaces().getFirst())));
		assertThat(c1.getImplementedInterfaces().getFirst(), is(not(sameInstance(c2.getImplementedInterfaces().getFirst()))));

		assertThat(c1.getImplementedInterfaces().getFirst().getResolvedApiType().get(), is(sameInstance(i1)));
		assertThat(c2.getImplementedInterfaces().getFirst().getResolvedApiType().get(), is(sameInstance(i2)));

		assertFalse(c1.findMethod("m()").isPresent());
		assertTrue(c2.findMethod("m()").isPresent());
	}

	@Test
	void unchanged_files_returns_same_API(@TempDir Path wd) throws Exception {
		Files.writeString(wd.resolve("A.java"), "public class A {}");
		Files.writeString(wd.resolve("B.java"), "public class B {}");

		var api1 = new JdtAPIExtractor().extractAPI(wd);

		var changedFiles = ChangedFiles.NO_CHANGES;
		var incrementalExtractor = new IncrementalJdtAPIExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1);

		assertThat(api1, is(sameInstance(api2)));
	}

	@Test
	void deleted_types_are_removed(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.java");
		Files.writeString(a, "public class A {}");
		Files.writeString(b, "public class B {}");

		var api1 = new JdtAPIExtractor().extractAPI(wd);

		var changedFiles = new ChangedFiles(Set.of(), Set.of(a), Set.of());
		var incrementalExtractor = new IncrementalJdtAPIExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1);

		assertClass(api1, "A");
		var b1 = assertClass(api1, "B");
		var b2 = assertClass(api2, "B");

		assertThat(api1.getAllTypes().count(), is(2L));
		assertThat(api2.getAllTypes().count(), is(1L));

		assertThat(b1, is(equalTo(b2)));
		assertThat(b1, is(not(sameInstance(b2))));
	}

	@Test
	void created_types_are_added(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		Files.writeString(a, "public class A {}");

		var api1 = new JdtAPIExtractor().extractAPI(wd);

		var b = wd.resolve("B.java");
		Files.writeString(b, "public class B {}");

		var changedFiles = new ChangedFiles(Set.of(), Set.of(), Set.of(b));
		var incrementalExtractor = new IncrementalJdtAPIExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1);

		var a1 = assertClass(api1, "A");
		var a2 = assertClass(api2, "A");
		assertClass(api2, "B");

		assertThat(api1.getAllTypes().count(), is(1L));
		assertThat(api2.getAllTypes().count(), is(2L));

		assertThat(a1, is(equalTo(a2)));
		assertThat(a1, is(not(sameInstance(a2))));
	}
}
