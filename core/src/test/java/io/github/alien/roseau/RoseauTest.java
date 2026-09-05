package io.github.alien.roseau;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.extractors.incremental.ChangedFiles;
import io.github.alien.roseau.extractors.incremental.HashFunction;
import io.github.alien.roseau.extractors.incremental.HashingChangedFilesProvider;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoseauTest {
	@Test
	void buildLibraryTypes_extracts_a_raw_snapshot(@TempDir Path wd) throws IOException {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public class A {}""");

		Library library = Library.of(wd);

		var types = Roseau.buildLibraryTypes(library);
		var api = Roseau.buildAPI(types);

		assertThat(types.getLibrary()).isEqualTo(library);
		assertThat(types.getAllTypes())
			.extracting(Symbol::getQualifiedName)
			.containsExactly("pkg.A");
		assertThat(api.getLibraryTypes()).isSameAs(types);
	}

	@Test
	void incrementalBuild_matches_full_rebuild(@TempDir Path wd) throws IOException {
		Path v1Dir = Files.createDirectory(wd.resolve("v1"));
		Path v2Dir = Files.createDirectory(wd.resolve("v2"));

		Files.writeString(v1Dir.resolve("A.java"), """
			public class A {}""");
		Files.writeString(v1Dir.resolve("B.java"), """
			public class B {}""");
		Files.writeString(v2Dir.resolve("A.java"), """
			public class A { public void m() {} }""");
		Files.writeString(v2Dir.resolve("B.java"), """
			public class B {}""");

		Library v1 = Library.of(v1Dir);
		var previousTypes = Roseau.buildLibraryTypes(v1);

		Library v2 = Library.of(v2Dir);
		var changedFiles = new HashingChangedFilesProvider(HashFunction.XXHASH)
			.getChangedFiles(v1.getLocation(), v2.getLocation());

		var incrementalTypes = Roseau.incrementalBuild(previousTypes, v2, changedFiles);
		var rebuiltTypes = Roseau.buildLibraryTypes(v2);

		assertThat(incrementalTypes).isEqualTo(rebuiltTypes);
	}

	@Test
	void incrementalDiff_unchanged_sources_use_the_new_classpath(@TempDir Path wd) throws IOException {
		var oldDependency = wd.resolve("old.jar");
		var newDependency = wd.resolve("new.jar");
		try (var _ = TestUtils.buildJar(Map.of("dependency.Base",
			"package dependency; public class Base { public void removed() {} }"), oldDependency);
		     var _ = TestUtils.buildJar(Map.of("dependency.Base",
			     "package dependency; public class Base {}"), newDependency)) {
			var sources = Files.createDirectory(wd.resolve("sources"));
			Files.writeString(sources.resolve("C.java"), "public class C extends dependency.Base {}");
			var oldLibrary = Library.builder().location(sources).classpath(List.of(oldDependency)).build();
			var newLibrary = Library.builder().location(sources).classpath(List.of(newDependency)).build();
			var oldTypes = Roseau.buildLibraryTypes(oldLibrary);

			var expected = Roseau.diff(oldLibrary, newLibrary);
			var actual = Roseau.incrementalDiff(oldLibrary, newLibrary);

			assertThat(expected.getBreakingChanges()).hasSize(1);
			assertThat(actual.getBreakingChanges()).isEqualTo(expected.getBreakingChanges());
			assertThat(actual.v2().getLibrary()).isEqualTo(newLibrary);
		}
	}

	@Test
	void incrementalBuild_unchanged_snapshot_carries_new_library_location(@TempDir Path wd) throws IOException {
		var oldLibrary = Library.of(Files.createDirectory(wd.resolve("old")));
		var newLibrary = Library.of(Files.createDirectory(wd.resolve("new")));
		var oldTypes = Roseau.buildLibraryTypes(oldLibrary);

		assertThat(Roseau.incrementalBuild(oldTypes, newLibrary, ChangedFiles.NO_CHANGES).getLibrary())
			.isEqualTo(newLibrary);
	}

	@Test
	void buildAPI_accepts_class_directories(@TempDir Path directory) throws Exception {
		var dependency = directory.resolve("Base.java");
		Files.writeString(dependency, "package dependency; public class Base { public void inherited() {} }");
		var classes = Files.createDirectory(directory.resolve("classes"));
		assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null,
			"-proc:none", "-d", classes.toString(), dependency.toString())).isZero();
		var sources = Files.createDirectory(directory.resolve("sources"));
		Files.writeString(sources.resolve("C.java"), "public class C extends dependency.Base {}");

		var library = Library.builder()
			.location(sources)
			.classpath(List.of(classes))
			.build();
		var api = Roseau.buildAPI(library);

		var type = api.findExportedType("C").orElseThrow();
		assertThat(api.analyzer().getExportedMethods(type))
			.extracting(MethodDecl::getQualifiedName).contains("dependency.Base.inherited()");
	}
}
