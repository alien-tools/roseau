package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.ExtractionGap;
import io.github.alien.roseau.options.RoseauOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.buildJarAPI;
import static io.github.alien.roseau.utils.TestUtils.buildSourcesAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Declarations that could not be extracted from sources make {@code --fail-on-unresolved} fail when they may affect
 * the API. See {@link io.github.alien.roseau.extractors.jdt.QualifiedParameterizedUnresolvedTypesTest} for detection.
 */
class ExtractionGapsTest {
	@Test
	void gap_of_an_exported_type_is_unresolved() {
		var api = buildSourcesAPI("""
			public class A {
				public unknown.Unknown<String> f;
			}""");

		assertThat(api.getExtractionGaps()).extracting(ExtractionGap::declaration).containsExactly("A.f");
		assertThat(api.getUnresolvedTypes()).containsExactly("unknown.Unknown");
	}

	@Test
	void gap_of_an_internal_type_is_ignored() {
		var api = buildSourcesAPI("""
			class Internal {
				public unknown.Unknown<String> f;
			}""");

		assertThat(api.getLibraryTypes().getExtractionGaps()).hasSize(1);
		assertThat(api.getExtractionGaps()).isEmpty();
		assertThat(api.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void gap_of_an_internal_supertype_of_an_exported_type_is_unresolved() {
		var api = buildSourcesAPI("""
			class Base implements Super {
				public unknown.Unknown<String> f;
			}
			interface Super {
				unknown.Other<String> m();
			}
			class Unrelated {
				public unknown.Unrelated<String> f;
			}
			public class A extends Base {}""");

		assertThat(api.getExtractionGaps()).extracting(ExtractionGap::declaration).containsExactly("Base.f", "Super.m()");
		assertThat(api.getUnresolvedTypes()).containsExactly("unknown.Other", "unknown.Unknown");
	}

	@Test
	void bytecode_has_no_gaps() {
		var api = buildJarAPI("""
			public class A {
				public java.util.List<String> f;
			}""");

		assertThat(api.getLibraryTypes().getExtractionGaps()).isEmpty();
	}

	@Test
	void check_fully_resolved_lists_gaps() {
		var v1 = buildSourcesAPI("""
			public class A {
				public unknown.Unknown<String> f;
			}""");
		var v2 = buildSourcesAPI("public class A {}");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getExtractionGaps()).extracting(ExtractionGap::declaration).containsExactly("A.f");
		assertThatThrownBy(report::checkFullyResolved)
			.isInstanceOf(RoseauException.class)
			.hasMessage("1 type(s) could not be resolved (unknown.Unknown); 1 declaration(s) could not be extracted: " +
				"A.f (A.java:2, unknown.Unknown); make sure the classpaths are accurate");
	}

	@Test
	void identical_gaps_of_both_versions_are_reported_once() {
		var src = """
			public class A {
				public unknown.Unknown<String> f;
			}""";

		var report = Roseau.diff(buildSourcesAPI(src), buildSourcesAPI(src));

		assertThat(report.getExtractionGaps()).hasSize(1);
		assertThat(report.getUnresolvedTypes()).containsExactly("unknown.Unknown");
	}

	@Test
	void gaps_of_excluded_types_are_ignored() {
		var src = """
			public class A {
				public unknown.Unknown<String> f;
			}
			public class B {
				public unknown.Other<String> f;
			}""";
		var exclude = new RoseauOptions.Exclude(List.of("A"), List.of());

		var report = Roseau.diff(buildSourcesAPI(src, exclude), buildSourcesAPI(src, exclude));

		assertThat(report.getExtractionGaps()).extracting(ExtractionGap::declaration).containsExactly("B.f");
		assertThat(report.getUnresolvedTypes()).containsExactly("unknown.Other");
	}

	@Test
	void excluded_declarations_are_ignored() {
		var src = """
			public class A {
				public unknown.Unknown<String> f;
			}""";
		var exclude = new RoseauOptions.Exclude(List.of("A\\.f"), List.of());

		var report = Roseau.diff(buildSourcesAPI(src, exclude), buildSourcesAPI(src, exclude));

		assertThatCode(report::checkFullyResolved).doesNotThrowAnyException();
	}

	@Test
	void recovered_types_are_still_reported_lazily() {
		var src = """
			public class A {
				public unknown.Unknown f;
				public unknown.Other<String> g;
			}""";

		var report = Roseau.diff(buildSourcesAPI(src), buildSourcesAPI(src));

		assertThat(report.getUnresolvedTypes()).containsExactly("unknown.Other");
	}

	@Test
	void identical_recovered_types_do_not_fail() {
		var src = """
			public class A {
				public unknown.Unknown f;
			}""";

		var report = Roseau.diff(buildSourcesAPI(src), buildSourcesAPI(src));

		assertThatCode(report::checkFullyResolved).doesNotThrowAnyException();
	}
}
