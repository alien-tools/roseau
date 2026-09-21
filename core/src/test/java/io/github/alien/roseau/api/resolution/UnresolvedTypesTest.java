package io.github.alien.roseau.api.resolution;

import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.buildSourcesAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnresolvedTypesTest {
	@Test
	void complete_api_has_no_unresolved_type() {
		var api = buildSourcesAPI("public class A {}");

		assertThat(api.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void missing_supertype_is_unresolved_as_soon_as_the_api_is_built() {
		var api = buildSourcesAPI("public class A extends unknown.Unknown {}");
		var a = assertClass(api, "A");

		// Hierarchies are resolved when the API is built, not when a query happens to walk them
		assertThat(api.getUnresolvedTypes()).containsExactly("unknown.Unknown");
		assertThat(api.analyzer().getAllSuperTypes(a))
			.extracting(TypeReference::getQualifiedName)
			.containsExactly("unknown.Unknown");
	}

	@Test
	void report_merges_unresolved_types_of_both_versions() {
		var v1 = buildSourcesAPI("public class A extends unknown.OnlyInV1 {}");
		var v2 = buildSourcesAPI("public class A extends unknown.OnlyInV2 {}");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getUnresolvedTypes()).contains("unknown.OnlyInV1", "unknown.OnlyInV2");
	}

	@Test
	void check_fully_resolved_passes_on_complete_apis() {
		var v1 = buildSourcesAPI("public class A {}");
		var v2 = buildSourcesAPI("public class A { public void m() {} }");

		var report = Roseau.diff(v1, v2);

		assertThatCode(report::checkFullyResolved).doesNotThrowAnyException();
	}

	@Test
	void check_fully_resolved_fails_on_incomplete_apis() {
		var v1 = buildSourcesAPI("public class A extends unknown.Unknown {}");
		var v2 = buildSourcesAPI("public class A {}");

		var report = Roseau.diff(v1, v2);

		assertThatThrownBy(report::checkFullyResolved)
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("unknown.Unknown")
			.hasMessageContaining("classpath");
	}

	@Test
	void diffing_does_not_record_types_that_only_exist_in_the_new_version() {
		var v1 = buildSourcesAPI("""
			public class A<T extends Number> {
				public void m(T t) {}
			}""");
		var v2 = buildSourcesAPI("""
			public class Base<T extends Number> {
				public void m(T t) {}
			}
			public class A<T extends Number> extends Base<T> {}""");

		var report = Roseau.diff(v1, v2);
		assertThat(report.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void diffing_does_not_record_types_that_only_exist_in_the_old_version() {
		var v1 = buildSourcesAPI("""
			public class Base<T extends Number> {
				public void m(T t) {}
			}
			public class A<T extends Number> extends Base<T> {}""");
		var v2 = buildSourcesAPI("""
			public class A<T extends Number> {
				public void m(T t) {}
			}""");

		var report = Roseau.diff(v1, v2);
		assertThat(report.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void diffing_does_not_record_a_return_type_the_new_version_dropped() {
		var v1 = buildSourcesAPI("""
			public class Old {}
			public class A {
				public Old m() { return null; }
			}""");
		var v2 = buildSourcesAPI("""
			public class A {
				public String m() { return null; }
			}""");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void diffing_does_not_record_a_field_type_the_new_version_dropped() {
		var v1 = buildSourcesAPI("""
			public class Old {}
			public class A {
				public Old f;
			}""");
		var v2 = buildSourcesAPI("""
			public class A {
				public String f;
			}""");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void diffing_does_not_record_a_type_parameter_bound_the_new_version_dropped() {
		var v1 = buildSourcesAPI("""
			public class Old {}
			public class A<T extends Old> {}""");
		var v2 = buildSourcesAPI("public class A<T extends String> {}");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void diffing_does_not_record_a_thrown_exception_the_new_version_dropped() {
		var v1 = buildSourcesAPI("""
			public class MyException extends Exception {}
			public class A {
				public void m() throws MyException {}
			}""");
		var v2 = buildSourcesAPI("""
			public class A {
				public void m() {}
			}""");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getUnresolvedTypes()).isEmpty();
	}

	@Test
	void type_the_old_version_references_and_cannot_resolve_is_still_reported() {
		var v1 = buildSourcesAPI("""
			public class A extends unknown.Unknown {
				public void m() {}
			}""");
		var v2 = buildSourcesAPI("""
			public class A {
				public void m() {}
			}""");

		var report = Roseau.diff(v1, v2);

		assertThat(report.getUnresolvedTypes()).containsExactly("unknown.Unknown");
	}

	@Test
	void unresolved_type_of_the_new_version_is_still_reported() {
		var v1 = buildSourcesAPI("""
			public class A {
				public String m() { return null; }
			}""");
		var v2 = buildSourcesAPI("""
			public class A {
				public unknown.Unknown m() { return null; }
			}""");

		var report = Roseau.diff(v1, v2);

		assertThat(v2.getUnresolvedTypes()).containsExactly("unknown.Unknown");
		assertThatThrownBy(report::checkFullyResolved)
			.isInstanceOf(RoseauException.class)
			.hasMessageContaining("unknown.Unknown");
	}
}
