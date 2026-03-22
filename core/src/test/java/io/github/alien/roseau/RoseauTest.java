package io.github.alien.roseau;

import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.extractors.incremental.HashFunction;
import io.github.alien.roseau.extractors.incremental.HashingChangedFilesProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
			.getChangedFiles(v1.location(), v2.location());

		var incrementalTypes = Roseau.incrementalBuild(previousTypes, v2, changedFiles);
		var rebuiltTypes = Roseau.buildLibraryTypes(v2);

		assertThat(incrementalTypes).isEqualTo(rebuiltTypes);
	}
}
