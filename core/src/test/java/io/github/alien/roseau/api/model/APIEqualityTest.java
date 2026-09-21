package io.github.alien.roseau.api.model;

import io.github.alien.roseau.utils.ApiBuilderType;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class APIEqualityTest {
	@Test
	void api_order_doesnt_matter() {
		var sources1 = """
			package p1;
			public class A {
				public int f;
				public void m() {}
			}
			public interface I {
				void m();
			}
			package p2;
			public class B {
				public static final String V = "x";
			}""";
		var sources2 = """
			package p2;
			public class B {
				public static final String V = "x";
			}
			package p1;
			public interface I {
				void m();
			}
			public class A {
				public void m() {}
				public int f;
			}""";

		var apis = Arrays.stream(ApiBuilderType.values())
			.flatMap(t -> Stream.of(sources1, sources2).map(t::build))
			.toList();

		var baseline = apis.getFirst();
		apis.forEach(api -> {
			assertThat(api).isEqualTo(baseline);
			assertThat(api.hashCode()).isEqualTo(baseline.hashCode());
		});
	}

	@Test
	void non_exported_symbols_do_not_affect_api_equality() {
		var api1 = TestUtils.buildSourcesAPI("""
			package p;
			public class A {
				public void m() {}
				private Object n() {}
			}
			class Hidden {
				int f;
			}""");
		var api2 = TestUtils.buildSourcesAPI("""
			package p;
			public class A {
				public void m() {}
				private String n() {}
				private int o() {}
			}
			class Hidden {
				String f;
				void n() {}
			}
			class AlsoHidden {}""");

		assertThat(api1).isEqualTo(api2);
		assertThat(api1.hashCode()).isEqualTo(api2.hashCode());
	}

	@Test
	void members_inherited_from_a_non_exported_supertype_affect_api_equality() {
		// Base is package-private, so it is not an exported type — but what A inherits from it is exported
		var api1 = TestUtils.buildSourcesAPI("""
			package p;
			class Base { public void m() {} public void n() {} }
			public class A extends Base {}""");
		var api2 = TestUtils.buildSourcesAPI("""
			package p;
			class Base { public void m() {} }
			public class A extends Base {}""");

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void modifiers_inherited_from_a_non_exported_supertype_affect_api_equality() {
		var api1 = TestUtils.buildSourcesAPI("""
			package p;
			class Base { public void m() {} }
			public class A extends Base {}""");
		var api2 = TestUtils.buildSourcesAPI("""
			package p;
			class Base { public final void m() {} }
			public class A extends Base {}""");

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void supertypes_gained_above_a_non_exported_supertype_affect_api_equality() {
		// A's own declaration does not change, but clients can now cast it to Serializable
		var api1 = TestUtils.buildSourcesAPI("""
			package p;
			class Base {}
			public class A extends Base {}""");
		var api2 = TestUtils.buildSourcesAPI("""
			package p;
			class Base implements java.io.Serializable {}
			public class A extends Base {}""");

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void package_private_members_affect_api_equality() {
		// They look invisible, but a package-private abstract method stops clients writing a concrete subclass
		var api1 = TestUtils.buildSourcesAPI("""
			package p;
			public abstract class A { public A() {} }""");
		var api2 = TestUtils.buildSourcesAPI("""
			package p;
			public abstract class A { public A() {} abstract void hidden(); }""");

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void a_classpath_that_changes_inherited_members_affects_api_equality() {
		// Identical sources: only the dependency the supertype comes from differs
		var sources = "public abstract class A extends depfixtures.abstractadded.B {}";
		var api1 = TestUtils.buildSourcesAPI(sources,
			List.of(Path.of("src/test/resources/dependency-classpath-v1.jar")));
		var api2 = TestUtils.buildSourcesAPI(sources,
			List.of(Path.of("src/test/resources/dependency-classpath-v2.jar")));

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void a_classpath_that_changes_nothing_visible_does_not_affect_api_equality() {
		var sources = "public class A { public void m() {} }";
		var api1 = TestUtils.buildSourcesAPI(sources,
			List.of(Path.of("src/test/resources/dependency-classpath-v1.jar")));
		var api2 = TestUtils.buildSourcesAPI(sources,
			List.of(Path.of("src/test/resources/dependency-classpath-v2.jar")));

		assertThat(api1).isEqualTo(api2);
		assertThat(api1.hashCode()).isEqualTo(api2.hashCode());
	}

	@Test
	void exported_symbol_changes_affect_api_equality() {
		var api1 = TestUtils.buildSourcesAPI("""
			package p;
			public class A {
				public void m() {}
			}""");
		var api2 = TestUtils.buildSourcesAPI("""
			package p;
			public class A {
				public int m() { return 0; }
			}""");

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void module_is_part_of_api_equality() {
		var api1 = TestUtils.buildSourcesAPI("""
			module m.one {
				exports p;
			}
			package p;
			public class A {}""");
		var api2 = TestUtils.buildSourcesAPI("""
			module m.two {
				exports p;
			}
			package p;
			public class A {}""");

		assertThat(api1).isNotEqualTo(api2);
	}

	@Test
	void unexported_packages_are_ignored_by_api_equality() {
		var api1 = TestUtils.buildSourcesAPI("""
			module m {
				exports p;
			}
			package p;
			public class A {}
			package q;
			public class Hidden {}""");
		var api2 = TestUtils.buildSourcesAPI("""
			module m {
				exports p;
			}
			package p;
			public class A {}
			package q;
			public class Hidden {
				public void m() {}
			}""");

		assertThat(api1).isEqualTo(api2);
	}

	@Test
	void exported_packages_affect_api_equality() {
		var api1 = TestUtils.buildSourcesAPI("""
			module m {
				exports p;
			}
			package p;
			public class A {}
			package q;
			public class B {}""");
		var api2 = TestUtils.buildSourcesAPI("""
			module m {
				exports p;
				exports q;
			}
			package p;
			public class A {}
			package q;
			public class B {}""");

		assertThat(api1).isNotEqualTo(api2);
	}
}
