package io.github.alien.roseau.api;

import io.github.alien.roseau.RoseauOptions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.buildJdtAPI;
import static org.assertj.core.api.Assertions.assertThat;

class LibraryTest {
	@Test
	void exclude_types_and_members_by_name() {
		var sources = """
			module m { exports p.api; exports p.internal; }
			package p.internal;
			public class Excluded { public void m() {} }
			package p.api;
			public class C {
				public void m() {}
				public void excluded() {}
			}""";

		var exclude = new RoseauOptions.Exclude(List.of("p\\.internal\\..*", "p\\.api\\.C\\.excluded\\(\\)"), List.of());
		API api = buildJdtAPI(sources, exclude);
		TypeDecl c = assertClass(api, "p.api.C");
		MethodDecl m = assertMethod(api, c, "m()");
		MethodDecl excludedMethod = assertMethod(api, c, "excluded()");
		TypeDecl excludedType = assertClass(api, "p.internal.Excluded");

		assertThat(api.isExcluded(c)).isFalse();
		assertThat(api.isExcluded(m)).isFalse();
		assertThat(api.isExcluded(excludedMethod)).isTrue();
		assertThat(api.isExcluded(excludedType)).isTrue();
	}

	@Test
	void name_exclusion_inherits() {
		var sources = """
			module m { exports p.api; }
			package p.api;
			public class Excluded {
				public static class Inner { public void m() {} }
				public void x() {}
			}""";

		var exclude = new RoseauOptions.Exclude(List.of("p\\.api\\.Excluded"), List.of());
		var api = buildJdtAPI(sources, exclude);
		var excluded = assertClass(api, "p.api.Excluded");
		var inner = assertClass(api, "p.api.Excluded$Inner");
		var x = assertMethod(api, excluded, "x()");
		var m = assertMethod(api, inner, "m()");

		assertThat(api.isExcluded(excluded)).isTrue();
		assertThat(api.isExcluded(inner)).isTrue();
		assertThat(api.isExcluded(m)).isTrue();
		assertThat(api.isExcluded(x)).isTrue();
	}

	@Test
	void exclude_types_and_members_by_annotation() {
		var sources = """
			module m { exports p.api; }
			package p.annotations;
			public @interface Internal {}
			package p.api;
			public class A {
				@p.annotations.Internal public void excluded() {}
			}
			@p.annotations.Internal public class B { public void m() {} }""";

		var exclude = new RoseauOptions.Exclude(
			List.of(),
			List.of(new RoseauOptions.AnnotationExclusion("p.annotations.Internal", Map.of())));

		var api = buildJdtAPI(sources, exclude);
		var a = assertClass(api, "p.api.A");
		var excluded = assertMethod(api, a, "excluded()");
		var b = assertClass(api, "p.api.B");
		var m = assertMethod(api, b, "m()");

		assertThat(api.isExcluded(a)).isFalse();
		assertThat(api.isExcluded(excluded)).isTrue();
		assertThat(api.isExcluded(b)).isTrue();
		assertThat(api.isExcluded(m)).isTrue();
	}

	@Test
	void exclude_annotation_values() {
		var sources = """
			module m { exports p.api; }
			package p.api;
			public @interface Internal { String level(); }
			public class C {
				@Internal(level = "alpha") public void alpha() {}
				@Internal(level = "beta") public void beta() {}
			}
			""";

		var excludes = new RoseauOptions.Exclude(
			List.of(),
			List.of(new RoseauOptions.AnnotationExclusion("p.api.Internal", Map.of("level", "alpha")))
		);

		var api = buildJdtAPI(sources, excludes);

		var c = assertClass(api, "p.api.C");
		var alpha = assertMethod(api, c, "alpha()");
		var beta = assertMethod(api, c, "beta()");

		assertThat(api.isExcluded(alpha)).isTrue();
		assertThat(api.isExcluded(beta)).isFalse();
	}
}
