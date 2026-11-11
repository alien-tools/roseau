package io.github.alien.roseau;

import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.utils.ReleaseJars;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.github.alien.roseau.diff.changes.BreakingChangeKind.EXECUTABLE_REMOVED;
import static io.github.alien.roseau.diff.changes.BreakingChangeKind.TYPE_REMOVED;
import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaReleaseAnalysisTest {
	@TempDir
	Path tempDir;

	// A client calling foo.reversed() against v1 fails on Java 8 with NoSuchMethodError against v2, and does not
	// recompile with --release 8. On Java 21+, the call resolves to the List.reversed() default method instead.
	@Test
	void removal_masked_by_a_newer_jdk_method_breaks_older_clients_only() {
		var v1 = jar("v1", 8, Map.of("p.Foo", """
			package p;
			public final class Foo extends java.util.ArrayList<String> {
				public java.util.List<String> reversed() { return this; }
			}"""));
		var v2 = jar("v2", 8, Map.of("p.Foo", """
			package p;
			public final class Foo extends java.util.ArrayList<String> {}"""));

		assertBC("p.Foo", "p.Foo.reversed()", EXECUTABLE_REMOVED, 3, diff(v1, v2, 8));
		assertNoBC(diff(v1, v2, 21));
	}

	@Test
	void analyzed_multi_release_jar_uses_the_entries_of_the_release() {
		var base = Map.of("p.C", """
			package p;
			public class C { public void m() {} }""");
		var v1 = multiReleaseJar("v1", base, 11, Map.of("p.C", """
			package p;
			public class C { public void m() {} public void n() {} }"""));
		var v2 = multiReleaseJar("v2", base, 11, Map.of("p.C", """
			package p;
			public class C { public void m() {} }"""));

		assertNoBC(diff(v1, v2, 8));
		assertBC("p.C", "p.C.n()", EXECUTABLE_REMOVED, 2, diff(v1, v2, 11));
	}

	// On Java 11, C inherits m() from the versioned dep.Base, so removing C.m() is compatible; on Java 8 it is not
	@Test
	void dependency_multi_release_jar_uses_the_entries_of_the_release() {
		var dependency = multiReleaseJar("dep", Map.of("dep.Base", """
			package dep;
			public class Base {}"""), 11, Map.of("dep.Base", """
			package dep;
			public class Base { public void m() {} }"""));
		var v1 = jar("v1", 8, List.of(dependency), Map.of("p.C", """
			package p;
			public class C extends dep.Base { public void m() {} }"""));
		var v2 = jar("v2", 8, List.of(dependency), Map.of("p.C", """
			package p;
			public class C extends dep.Base {}"""));

		assertBC("p.C", "p.C.m()", EXECUTABLE_REMOVED, 2,
			diff(library(v1, 8, List.of(dependency)), library(v2, 8, List.of(dependency))));
		assertNoBC(diff(library(v1, 11, List.of(dependency)), library(v2, 11, List.of(dependency))));
	}

	// Java 8 clients see no module: b.B is part of their API even though the Java 9 descriptor does not export it
	@Test
	void versioned_module_descriptor_does_not_apply_to_java_8_clients() {
		var v1 = moduleJar("v1", true, Map.of(
			"a.A", "package a; public class A {}",
			"b.B", "package b; public class B {}"));
		var v2 = moduleJar("v2", true, Map.of(
			"a.A", "package a; public class A {}"));

		assertBC("b.B", "b.B", TYPE_REMOVED, -1, diff(v1, v2, 8));
		assertNoBC(diff(v1, v2, 11));
		assertBC("b.B", "b.B", TYPE_REMOVED, -1,
			diff(library(v1, 11, List.of(), true), library(v2, 11, List.of(), true)));
	}

	@Test
	void root_module_descriptor_does_not_apply_to_java_8_clients() {
		var v1 = moduleJar("v1", false, Map.of(
			"a.A", "package a; public class A {}",
			"b.B", "package b; public class B {}"));
		var v2 = moduleJar("v2", false, Map.of(
			"a.A", "package a; public class A {}"));

		assertBC("b.B", "b.B", TYPE_REMOVED, -1, diff(v1, v2, 8));
		assertNoBC(diff(v1, v2, 11));
	}

	@Test
	void comparing_libraries_of_different_releases_is_refused() {
		var v1 = jar("v1", 8, Map.of("p.C", "package p; public class C {}"));
		var v2 = jar("v2", 8, Map.of("p.C", "package p; public class C {}"));
		var java8 = Library.builder().location(v1).javaRelease(8).build();
		var java21 = Library.builder().location(v2).javaRelease(21).build();

		assertThatThrownBy(() -> Roseau.diff(java8, java21))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("both versions must be compared on the same Java release");
		assertThatThrownBy(() -> Roseau.diff(Roseau.buildAPI(java8), Roseau.buildAPI(java21)))
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("both versions must be compared on the same Java release");
	}

	private List<BreakingChange> diff(Path v1, Path v2, int release) {
		return diff(library(v1, release, List.of()), library(v2, release, List.of()));
	}

	private static List<BreakingChange> diff(Library v1, Library v2) {
		return Roseau.diff(v1, v2).getBreakingChanges();
	}

	private static Library library(Path jar, int release, List<Path> classpath) {
		return library(jar, release, classpath, false);
	}

	private static Library library(Path jar, int release, List<Path> classpath, boolean ignoreModule) {
		return Library.builder()
			.location(jar)
			.classpath(classpath)
			.ignoreModule(ignoreModule)
			.javaRelease(release)
			.build();
	}

	private Path jar(String name, int release, Map<String, String> sources) {
		return jar(name, release, List.of(), sources);
	}

	private Path jar(String name, int release, List<Path> classpath, Map<String, String> sources) {
		Path classes = ReleaseJars.compile(tempDir.resolve(name), release, classpath, sources);
		return ReleaseJars.jar(tempDir.resolve(name + ".jar"), false, ReleaseJars.entries(classes, ""));
	}

	private Path multiReleaseJar(String name, Map<String, String> base, int version, Map<String, String> versioned) {
		Path baseClasses = ReleaseJars.compile(tempDir.resolve(name + "-base"), 8, List.of(), base);
		Path versionedClasses = ReleaseJars.compile(tempDir.resolve(name + "-" + version), version, List.of(), versioned);
		Map<String, Path> entries = new LinkedHashMap<>(ReleaseJars.entries(baseClasses, ""));
		entries.putAll(ReleaseJars.entries(versionedClasses, "META-INF/versions/" + version + "/"));
		return ReleaseJars.jar(tempDir.resolve(name + ".jar"), true, entries);
	}

	// Java 8 classes, plus a module m exporting package a compiled for Java 9, at the root or as a versioned entry
	private Path moduleJar(String name, boolean versioned, Map<String, String> sources) {
		Path baseClasses = ReleaseJars.compile(tempDir.resolve(name + "-base"), 8, List.of(), sources);
		Map<String, String> moduleSources = new LinkedHashMap<>(sources);
		moduleSources.put("module-info", "module m { exports a; }");
		Path moduleClasses = ReleaseJars.compile(tempDir.resolve(name + "-module"), 9, List.of(), moduleSources);

		Map<String, Path> entries = new LinkedHashMap<>(ReleaseJars.entries(baseClasses, ""));
		entries.put(versioned ? "META-INF/versions/9/module-info.class" : "module-info.class",
			moduleClasses.resolve("module-info.class"));
		return ReleaseJars.jar(tempDir.resolve(name + ".jar"), versioned, entries);
	}
}
