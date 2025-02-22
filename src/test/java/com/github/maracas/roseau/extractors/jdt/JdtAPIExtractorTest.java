package com.github.maracas.roseau.extractors.jdt;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.extractors.APIExtractionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class JdtAPIExtractorTest {
	@TempDir
	Path wd;

	@Test
	void parse_null_throws() {
		var extractor = new JdtAPIExtractor();
		assertThrows(NullPointerException.class, () -> extractor.extractAPI(null));
	}

	@Test
	void parse_invalid_location_throws() {
		var extractor = new JdtAPIExtractor();
		assertThrows(APIExtractionException.class, () -> extractor.extractAPI(Path.of("invalid")));
	}

	@Test
	void parse_empty_sources_empty_api() {
		var extractor = new JdtAPIExtractor();
		var api = extractor.extractAPI(wd);

		assertThat(api, is(notNullValue()));
		assertThat(api.getAllTypes().count(), is(0L));
	}

	@Test
	void parse_valid_file_creates_api() throws Exception {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public class A {}""");

		var extractor = new JdtAPIExtractor();
		var api = extractor.extractAPI(wd);

		assertThat(api.getAllTypes().count(), is(1L));
		assertThat(api.getAllTypes().toList().getFirst().getQualifiedName(), is("pkg.A"));
	}

	@Test
	void parse_syntax_error_ignores() throws Exception {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			public clazz A {}""");
		Files.writeString(wd.resolve("B.java"), """
			package pkg;
			public class B {
				public int float double f;
			}""");

		var extractor = new JdtAPIExtractor();
		var api = extractor.extractAPI(wd);

		assertThat(api, is(notNullValue()));
		assertThat(api.getAllTypes().count(), is(1L));

		var b = api.findType("pkg.B").orElseThrow();
		assertThat(b.getDeclaredFields(), is(empty()));
	}

	@Test
	void unresolved_symbols_have_fqns() throws Exception {
		Files.writeString(wd.resolve("A.java"), """
			package pkg;
			import unknown.B;
			import unknown.D;
			import java.util.List;
			public class A extends unknown.A implements B {
				public List<unknown.C> f;
				public D m(unknown.E[] p1, F p2) { return null; }
			}""");

		var extractor = new JdtAPIExtractor();
		var api = extractor.extractAPI(wd);

		assertThat(api, is(notNullValue()));
		assertThat(api.getAllTypes().count(), is(1L));

		var a = api.findType("pkg.A");
		if (a.isPresent() && a.get() instanceof ClassDecl cls) {
			assertThat(cls.getSuperClass().getQualifiedName(), is(equalTo("unknown.A")));
			assertThat(cls.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("unknown.B")));
			assertThat(cls.getDeclaredFields().getFirst().getType().getQualifiedName(), is(equalTo("java.util.List")));
			assertThat(cls.getDeclaredMethods().getFirst().getType().getQualifiedName(), is(equalTo("unknown.D")));
			assertThat(cls.getDeclaredMethods().getFirst().getErasure(), is(equalTo("m(unknown.E[],pkg.F)")));
		} else fail();
	}
}
