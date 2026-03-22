package io.github.alien.roseau;

import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.extractors.incremental.HashFunction;
import io.github.alien.roseau.extractors.incremental.HashingChangedFilesProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
			.getChangedFiles(v1.location(), v2.location());

		var incrementalTypes = Roseau.incrementalBuild(previousTypes, v2, changedFiles);
		var rebuiltTypes = Roseau.buildLibraryTypes(v2);

		assertThat(incrementalTypes).isEqualTo(rebuiltTypes);
	}

	@Test
	void diff_request_applies_policy(@TempDir Path wd) throws IOException {
		Path v1Dir = Files.createDirectory(wd.resolve("v1"));
		Path v2Dir = Files.createDirectory(wd.resolve("v2"));
		Files.writeString(v1Dir.resolve("A.java"), """
			package pkg;
			public class A { public void m() {} }""");
		Files.writeString(v1Dir.resolve("B.java"), """
			package pkg;
			public class B { public void n() {} }""");
		Files.writeString(v2Dir.resolve("A.java"), """
			package pkg;
			public class A {}""");
		Files.writeString(v2Dir.resolve("B.java"), """
			package pkg;
			public class B {}""");

		Library v1 = Library.of(v1Dir);
		Library v2 = Library.of(v2Dir);
		var policy = DiffPolicy.builder().excludeNames(List.of(Pattern.compile("pkg\\.A"))).build();
		var report = Roseau.diff(new DiffRequest(v1, v2, policy));

		assertThat(report.breakingChanges())
			.allSatisfy(bc -> assertThat(bc.impactedType().getQualifiedName()).isEqualTo("pkg.B"));
	}

	@Test
	void diff_library_overload_uses_empty_policy(@TempDir Path wd) throws IOException {
		Path v1Dir = Files.createDirectory(wd.resolve("v1"));
		Path v2Dir = Files.createDirectory(wd.resolve("v2"));
		Files.writeString(v1Dir.resolve("A.java"), """
			package pkg;
			public class A { public void m() {} }""");
		Files.writeString(v2Dir.resolve("A.java"), """
			package pkg;
			public class A {}""");

		Library v1 = Library.of(v1Dir);
		Library v2 = Library.of(v2Dir);

		assertThat(Roseau.diff(v1, v2))
			.isEqualTo(Roseau.diff(new DiffRequest(v1, v2, DiffPolicy.empty())));
	}

	@Test
	void incrementalDiff_matches_full_diff(@TempDir Path wd) throws IOException {
		Path v1Dir = Files.createDirectory(wd.resolve("v1"));
		Path v2Dir = Files.createDirectory(wd.resolve("v2"));
		Files.writeString(v1Dir.resolve("A.java"), """
			public class A { public void m() {} }""");
		Files.writeString(v1Dir.resolve("B.java"), """
			public class B {}""");
		Files.writeString(v2Dir.resolve("A.java"), """
			public class A {}""");
		Files.writeString(v2Dir.resolve("B.java"), """
			public class B {}""");

		Library v1 = Library.of(v1Dir);
		Library v2 = Library.of(v2Dir);

		var full = Roseau.diff(v1, v2);
		var incremental = Roseau.incrementalDiff(v1, v2);

		assertThat(full).isEqualTo(incremental);
	}
}
