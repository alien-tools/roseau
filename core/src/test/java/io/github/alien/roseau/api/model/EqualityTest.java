package io.github.alien.roseau.api.model;

import com.cedarsoftware.util.DeepEquals;
import io.github.alien.roseau.utils.ApiBuilderType;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EqualityTest {
	@Test
	void api_order_doesnt_matter() {
		var sources1 = """
			package p1;
			public class C {
				public static class A {}
				public static class B {}
				public int f1;
				public int f2;
				public E m1(int i) { return E.A; }
				public E m2() { return E.B; }
			}
			public enum E { A, B; }
			package p2;
			@FunctionalInterface
			@Deprecated
			public interface I { void m(String s); }
			public class C {}""";
		var sources2 = """
			package p2;
			public class C {}
			@FunctionalInterface
			@Deprecated
			public interface I { void m(String s); }
			package p1;
			public enum E { B, A; }
			public class C {
				public E m2() { return E.A; }
				public E m1(int i) { return E.B; }
				public int f2;
				public int f1;
				public static class B {}
				public static class A {}
			}""";

		var apis = Arrays.stream(ApiBuilderType.values())
			.flatMap(t -> Stream.of(sources1, sources2).map(t::build))
			.toList();

		var baseline = apis.getFirst();
		apis.forEach(api -> {
			var apiTypes = api.getLibraryTypes().getAllTypes();
			var baselineTypes = baseline.getLibraryTypes().getAllTypes();
			var opts = new HashMap<String, Object>();
			boolean equals = DeepEquals.deepEquals(apiTypes, baselineTypes, opts);
			assertThat(equals).as(opts.toString()).isTrue();
			assertThat(apiTypes).isEqualTo(baselineTypes);
		});
	}
}
