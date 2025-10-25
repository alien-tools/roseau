package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static org.assertj.core.api.Assertions.assertThat;

class IncrementalJdtTypesExtractorTest {
	@Test
	void unchanged_symbols_are_kept(@TempDir Path wd) throws Exception {
		var i = wd.resolve("I.java");
		var library1 = Library.of(wd);
		Files.writeString(i, "public interface I {}");
		Files.writeString(wd.resolve("C.java"), "public class C implements I {}");
		Files.writeString(wd.resolve("R.java"), "public record R() {}");

		var extractor = new JdtTypesExtractor();
		var types1 = extractor.extractTypes(library1);
		var api1 = types1.toAPI();

		Files.writeString(i, "public interface I { void m(); }");
		var changedFiles = new ChangedFiles(Set.of(wd.relativize(i)), Set.of(), Set.of());

		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var types2 = incrementalExtractor.incrementalUpdate(types1, Library.of(wd), changedFiles);
		var api2 = types2.toAPI();

		var i1 = assertInterface(api1, "I");
		var i2 = assertInterface(api2, "I");
		var c1 = assertClass(api1, "C");
		var c2 = assertClass(api2, "C");
		var r1 = assertRecord(api1, "R");
		var r2 = assertRecord(api2, "R");

		assertThat(i1).isNotEqualTo(i2);
		assertThat(c1).isSameAs(c2);
		assertThat(r1).isSameAs(r2);

		assertThat(c1.getImplementedInterfaces())
			.singleElement()
			.isSameAs(c2.getImplementedInterfaces().iterator().next());

		assertThat(api1.resolver().resolve(c1.getImplementedInterfaces().iterator().next())).containsSame(i1);
		assertThat(api2.resolver().resolve(c2.getImplementedInterfaces().iterator().next())).containsSame(i2);

		assertThat(api1.findMethod(c1, "m()")).isEmpty();
		assertThat(api2.findMethod(c2, "m()")).isPresent();
	}

	@Test
	void unchanged_files_returns_same_API(@TempDir Path wd) throws Exception {
		Files.writeString(wd.resolve("A.java"), "public class A {}");
		Files.writeString(wd.resolve("B.java"), "public class B {}");

		var types1 = new JdtTypesExtractor().extractTypes(Library.of(wd));

		var changedFiles = ChangedFiles.NO_CHANGES;
		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var types2 = incrementalExtractor.incrementalUpdate(types1, Library.of(wd), changedFiles);

		assertThat(types1).isSameAs(types2);
	}

	@Test
	void deleted_types_are_removed(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		var b = wd.resolve("B.java");
		Files.writeString(a, "public class A {}");
		Files.writeString(b, "public class B {}");

		var api1 = new JdtTypesExtractor().extractTypes(Library.of(wd)).toAPI();

		var changedFiles = new ChangedFiles(Set.of(), Set.of(wd.relativize(a)), Set.of());
		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var api2 = incrementalExtractor.incrementalUpdate(api1.getLibraryTypes(), Library.of(wd),
			changedFiles).toAPI();

		assertClass(api1, "A");
		var b1 = assertClass(api1, "B");
		var b2 = assertClass(api2, "B");

		assertThat(api1.getExportedTypes()).hasSize(2);
		assertThat(api2.getExportedTypes()).hasSize(1);

		assertThat(b1).isSameAs(b2);
	}

	@Test
	void created_types_are_added(@TempDir Path wd) throws Exception {
		var a = wd.resolve("A.java");
		Files.writeString(a, "public class A {}");

		var api1 = new JdtTypesExtractor().extractTypes(Library.of(wd)).toAPI();

		var b = wd.resolve("B.java");
		Files.writeString(b, "public class B {}");

		var changedFiles = new ChangedFiles(Set.of(), Set.of(), Set.of(wd.relativize(b)));
		var incrementalExtractor = new IncrementalJdtTypesExtractor();
		var api2 = incrementalExtractor.incrementalUpdate(api1.getLibraryTypes(), Library.of(wd), changedFiles).toAPI();

		var a1 = assertClass(api1, "A");
		var a2 = assertClass(api2, "A");
		assertClass(api2, "B");

		assertThat(api1.getExportedTypes()).hasSize(1);
		assertThat(api2.getExportedTypes()).hasSize(2);

		assertThat(a1).isSameAs(a2);
	}

	@Test
	void can_parse_files_in_isolation(@TempDir Path wd) throws Exception {
		var root = Files.createDirectories(wd.resolve("src/main/java/"));
		var a = Files.createDirectories(root.resolve("pkg1")).resolve("A.java");
		var b = Files.createDirectories(root.resolve("pkg2")).resolve("B.java");
		var c = Files.createDirectories(root.resolve("pkg3")).resolve("C.java");
		var d = Files.createDirectories(root.resolve("pkg2")).resolve("D.java");
		Files.writeString(a, """
			package pkg1;
			public class A {}""");
		Files.writeString(b, """
			package pkg2;
			import pkg1.A;
			import pkg3.C;
			public class B<T> extends C<B> {
				public C c = null;
				public A m() { return null; }
				public <U> C<U> n(C<U> p1, C<T> p2) { return null; }
				public D o(java.util.List<D> p) { return null; }
			}""");
		Files.writeString(c, """
			package pkg3;
			public class C<T> extends pkg1.A {
				public pkg1.A a = null;
			}""");
		Files.writeString(d, """
			package pkg2;
			class D {}""");

		var api1 = new JdtTypesExtractor().extractTypes(Library.of(root)).toAPI();
		assertThat(api1.getExportedTypes()).hasSize(3);

		Files.writeString(b, """
			package pkg2;
			import pkg1.A;
			import pkg3.C;
			class B<T extends java.lang.CharSequence> extends A {
				public <U extends A> C<U> p(D p1, A p2) { return null; }
			}""");

		var changedFiles = new ChangedFiles(Set.of(root.relativize(b)), Set.of(), Set.of());
		var incrementalExtractor = new IncrementalJdtTypesExtractor();

		var api2 = incrementalExtractor.incrementalUpdate(api1.getLibraryTypes(), Library.of(root), changedFiles).toAPI();
		assertThat(api2.getExportedTypes()).hasSize(2);

		var clsB = assertClass(api2, "pkg2.B");
		assertThat(clsB.getSuperClass()).isEqualTo(new TypeReference<>("pkg1.A"));
		assertThat(clsB.getFormalTypeParameters())
			.singleElement()
			.extracting(ftp -> ftp.bounds().getFirst())
			.isEqualTo(new TypeReference<>("java.lang.CharSequence"));
		assertThat(clsB.getDeclaredMethods()).hasSize(1);
		assertThat(clsB.getDeclaredMethods().iterator().next().getFormalTypeParameters())
			.singleElement()
			.extracting(ftp -> ftp.bounds().getFirst())
			.isEqualTo(new TypeReference<>("pkg1.A"));
		assertThat(clsB.getDeclaredMethods().iterator().next().getType()).isEqualTo(
			new TypeReference<>("pkg3.C", List.of(new TypeParameterReference("U"))));
		assertThat(clsB.getDeclaredMethods().iterator().next().getParameters().getFirst().type())
			.isEqualTo(new TypeReference<>("pkg2.D"));
		assertThat(clsB.getDeclaredMethods().iterator().next().getParameters().get(1).type())
			.isEqualTo(new TypeReference<>("pkg1.A"));
	}
}
