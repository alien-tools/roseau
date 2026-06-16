package io.github.alien.roseau.extractors;

import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsOverloadingTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void overloading_methods(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
				public void m(int a) {}
				public void m(String s) {}
				public void m(int a, String s) {}
				public void m(String s, int a) {}
			}""");

		var a = assertClass(api, "A");
		assertThat(a.getDeclaredMethods()).hasSize(5);

		for (var m : a.getDeclaredMethods()) {
			for (var n : a.getDeclaredMethods()) {
				if (m == n) {
					assertFalse(api.analyzer().isOverloading(m, n), m + " does overload " + n);
					assertTrue(api.analyzer().isOverriding(m, n), m + " does not override " + n);
				} else {
					assertTrue(api.analyzer().isOverloading(m, n), m + " does not overload " + n);
					assertFalse(api.analyzer().isOverriding(m, n), m + " does override " + n);
				}
			}
		}
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void overloading_methods_hierarchy(ApiBuilder builder) {
		var api = builder.build("""
			public interface I {
				void m();
			}
			public abstract class A implements I {
				public abstract void m(int a);
				public void m(String a) {}
			}
			public class C extends A {
				public void m() {}
				public void m(int a) {}
				public void m(double a) {}
			}""");

		var i = assertInterface(api, "I");
		var a = assertClass(api, "A");
		var c = assertClass(api, "C");

		assertThat(i.getDeclaredMethods()).hasSize(1);
		assertThat(a.getDeclaredMethods()).hasSize(2);
		assertThat(c.getDeclaredMethods()).hasSize(3);

		assertThat(api.analyzer().getExportedMethods(i)).hasSize(1 + 11); // java.lang.Object's methods
		assertThat(api.analyzer().getExportedMethods(a)).hasSize(3 + 11); // java.lang.Object's methods
		assertThat(api.analyzer().getExportedMethods(c)).hasSize(4 + 11); // java.lang.Object's methods

		var im = assertMethod(api, i, "m()");
		var amInt = assertMethod(api, a, "m(int)");
		var amString = assertMethod(api, a, "m(java.lang.String)");
		var cm = assertMethod(api, c, "m()");
		var cmInt = assertMethod(api, c, "m(int)");
		var cmDouble = assertMethod(api, c, "m(double)");

		assertFalse(api.analyzer().isOverloading(im, im));
		assertTrue(api.analyzer().isOverloading(im, amInt));
		assertTrue(api.analyzer().isOverloading(im, amString));
		assertFalse(api.analyzer().isOverloading(im, cm));
		assertTrue(api.analyzer().isOverloading(im, cmInt));
		assertTrue(api.analyzer().isOverloading(im, cmDouble));

		assertTrue(api.analyzer().isOverloading(amInt, im));
		assertFalse(api.analyzer().isOverloading(amInt, amInt));
		assertTrue(api.analyzer().isOverloading(amInt, amString));
		assertTrue(api.analyzer().isOverloading(amInt, cm));
		assertFalse(api.analyzer().isOverloading(amInt, cmInt));
		assertTrue(api.analyzer().isOverloading(amInt, cmDouble));

		assertTrue(api.analyzer().isOverloading(amString, im));
		assertTrue(api.analyzer().isOverloading(amString, amInt));
		assertFalse(api.analyzer().isOverloading(amString, amString));
		assertTrue(api.analyzer().isOverloading(amString, cm));
		assertTrue(api.analyzer().isOverloading(amString, cmInt));
		assertTrue(api.analyzer().isOverloading(amString, cmDouble));

		assertFalse(api.analyzer().isOverloading(cm, im));
		assertTrue(api.analyzer().isOverloading(cm, amInt));
		assertTrue(api.analyzer().isOverloading(cm, amString));
		assertFalse(api.analyzer().isOverloading(cm, cm));
		assertTrue(api.analyzer().isOverloading(cm, cmInt));
		assertTrue(api.analyzer().isOverloading(cm, cmDouble));

		assertTrue(api.analyzer().isOverloading(cmInt, im));
		assertFalse(api.analyzer().isOverloading(cmInt, amInt));
		assertTrue(api.analyzer().isOverloading(cmInt, amString));
		assertTrue(api.analyzer().isOverloading(cmInt, cm));
		assertFalse(api.analyzer().isOverloading(cmInt, cmInt));
		assertTrue(api.analyzer().isOverloading(cmInt, cmDouble));

		assertTrue(api.analyzer().isOverloading(cmDouble, im));
		assertTrue(api.analyzer().isOverloading(cmDouble, amInt));
		assertTrue(api.analyzer().isOverloading(cmDouble, amString));
		assertTrue(api.analyzer().isOverloading(cmDouble, cm));
		assertTrue(api.analyzer().isOverloading(cmDouble, cmInt));
		assertFalse(api.analyzer().isOverloading(cmDouble, cmDouble));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void overloading_varargs(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m(int a) {}
				public void m(int... a) {}
			}""");

		var a = assertClass(api, "A");
		assertThat(a.getDeclaredMethods()).hasSize(2);

		var m1 = assertMethod(api, a, "m(int)");
		var m2 = assertMethod(api, a, "m(int[])");

		assertFalse(api.analyzer().isOverloading(m1, m1));
		assertTrue(api.analyzer().isOverloading(m1, m2));
		assertTrue(api.analyzer().isOverloading(m2, m1));
		assertFalse(api.analyzer().isOverloading(m2, m2));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void overloading_erasure(ApiBuilder builder) {
		var api = builder.build("""
			public class A<V extends CharSequence> {
				public <T extends CharSequence> void m(T a) {}
				public <T extends Number> void m(T a) {}
				public <T> void m(T a) {}
			}""");

		var a = assertClass(api, "A");
		assertThat(a.getDeclaredMethods()).hasSize(3);

		var m1 = assertMethod(api, a, "m(java.lang.CharSequence)");
		var m2 = assertMethod(api, a, "m(java.lang.Number)");
		var m3 = assertMethod(api, a, "m(java.lang.Object)");

		assertTrue(api.analyzer().isOverloading(m1, m2));
		assertTrue(api.analyzer().isOverloading(m1, m3));
		assertTrue(api.analyzer().isOverloading(m2, m1));
		assertTrue(api.analyzer().isOverloading(m2, m3));
		assertTrue(api.analyzer().isOverloading(m3, m1));
		assertTrue(api.analyzer().isOverloading(m3, m2));
	}
}
