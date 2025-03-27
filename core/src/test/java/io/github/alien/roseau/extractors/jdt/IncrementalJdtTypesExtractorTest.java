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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalJdtTypesExtractorTest {
	@Test
	void unchanged_symbols_are_kept(@TempDir Path wd) throws Exception {
		var i = wd.resolve("I.java");
		Files.writeString(i, "public interface I {}");
		Files.writeString(wd.resolve("C.java"), "public class C implements I {}");
		Files.writeString(wd.resolve("R.java"), "public record R() {}");

		var extractor = new JdtTypesExtractor();
		var types1 = extractor.extractTypes(wd);
		var api1 = types1.toAPI();

		Files.writeString(i, "public interface I { void m(); }");
		var changedFiles = new ChangedFiles(Set.of(i), Set.of(), Set.of());

		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var types2 = incrementalExtractor.refreshAPI(wd, changedFiles, types1);
		var api2 = types2.toAPI();

		var i1 = assertInterface(api1, "I");
		var i2 = assertInterface(api2, "I");
		var c1 = assertClass(api1, "C");
		var c2 = assertClass(api2, "C");
		var r1 = assertRecord(api1, "R");
		var r2 = assertRecord(api2, "R");

		assertThat(i1, is(not(equalTo(i2))));
		assertThat(c1, is(sameInstance(c2)));
		assertThat(r1, is(sameInstance(r2)));

		assertThat(c1.getImplementedInterfaces().getFirst(), is(sameInstance(c2.getImplementedInterfaces().getFirst())));

		assertThat(api1.resolver().resolve(c1.getImplementedInterfaces().getFirst()).get(), is(sameInstance(i1)));
		assertThat(api2.resolver().resolve(c2.getImplementedInterfaces().getFirst()).get(), is(sameInstance(i2)));

		assertFalse(api1.findMethod(c1, "m()").isPresent());
		assertTrue(api2.findMethod(c2, "m()").isPresent());
	}

	@Test
	void unchanged_files_returns_same_API(@TempDir Path wd) throws Exception {
		Files.writeString(wd.resolve("A.java"), "public class A {}");
		Files.writeString(wd.resolve("B.java"), "public class B {}");

		var api1 = new JdtTypesExtractor().extractTypes(wd);

		var changedFiles = ChangedFiles.NO_CHANGES;
		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1);

		assertThat(api1, is(sameInstance(api2)));
	}

	@Test
	void deleted_types_are_removed(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.java");
		Files.writeString(a, "public class A {}");
		Files.writeString(b, "public class B {}");

		var api1 = new JdtTypesExtractor().extractTypes(wd).toAPI();

		var changedFiles = new ChangedFiles(Set.of(), Set.of(a), Set.of());
		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1.getTypes()).toAPI();

		assertClass(api1, "A");
		var b1 = assertClass(api1, "B");
		var b2 = assertClass(api2, "B");

		assertThat(api1.getExportedTypes(), hasSize(2));
		assertThat(api2.getExportedTypes(), hasSize(1));

		assertThat(b1, is(sameInstance(b2)));
	}

	@Test
	void created_types_are_added(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		Files.writeString(a, "public class A {}");

		var api1 = new JdtTypesExtractor().extractTypes(wd).toAPI();

		var b = wd.resolve("B.java");
		Files.writeString(b, "public class B {}");

		var changedFiles = new ChangedFiles(Set.of(), Set.of(), Set.of(b));
		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var api2 = incrementalExtractor.refreshAPI(wd, changedFiles, api1.getTypes()).toAPI();

		var a1 = assertClass(api1, "A");
		var a2 = assertClass(api2, "A");
		assertClass(api2, "B");

		assertThat(api1.getExportedTypes(), hasSize(1));
		assertThat(api2.getExportedTypes(), hasSize(2));

		assertThat(a1, is(sameInstance(a2)));
	}
}
