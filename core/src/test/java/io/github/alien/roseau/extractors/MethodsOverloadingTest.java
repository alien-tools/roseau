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
					assertFalse(api.isOverloading(m, n), m + " does overload " + n);
					assertTrue(api.isOverriding(m, n), m + " does not override " + n);
				} else {
					assertTrue(api.isOverloading(m, n), m + " does not overload " + n);
					assertFalse(api.isOverriding(m, n), m + " does override " + n);
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

		assertThat(api.getAllMethods(i)).hasSize(1 + 11); // java.lang.Object's methods
		assertThat(api.getAllMethods(a)).hasSize(3 + 11); // java.lang.Object's methods
		assertThat(api.getAllMethods(c)).hasSize(4 + 11); // java.lang.Object's methods

		var im = assertMethod(api, i, "m()");
		var amInt = assertMethod(api, a, "m(int)");
		var amString = assertMethod(api, a, "m(java.lang.String)");
		var cm = assertMethod(api, c, "m()");
		var cmInt = assertMethod(api, c, "m(int)");
		var cmDouble = assertMethod(api, c, "m(double)");

		assertFalse(api.isOverloading(im, im));
		assertTrue(api.isOverloading(im, amInt));
		assertTrue(api.isOverloading(im, amString));
		assertFalse(api.isOverloading(im, cm));
		assertTrue(api.isOverloading(im, cmInt));
		assertTrue(api.isOverloading(im, cmDouble));

		assertTrue(api.isOverloading(amInt, im));
		assertFalse(api.isOverloading(amInt, amInt));
		assertTrue(api.isOverloading(amInt, amString));
		assertTrue(api.isOverloading(amInt, cm));
		assertFalse(api.isOverloading(amInt, cmInt));
		assertTrue(api.isOverloading(amInt, cmDouble));

		assertTrue(api.isOverloading(amString, im));
		assertTrue(api.isOverloading(amString, amInt));
		assertFalse(api.isOverloading(amString, amString));
		assertTrue(api.isOverloading(amString, cm));
		assertTrue(api.isOverloading(amString, cmInt));
		assertTrue(api.isOverloading(amString, cmDouble));

		assertFalse(api.isOverloading(cm, im));
		assertTrue(api.isOverloading(cm, amInt));
		assertTrue(api.isOverloading(cm, amString));
		assertFalse(api.isOverloading(cm, cm));
		assertTrue(api.isOverloading(cm, cmInt));
		assertTrue(api.isOverloading(cm, cmDouble));

		assertTrue(api.isOverloading(cmInt, im));
		assertFalse(api.isOverloading(cmInt, amInt));
		assertTrue(api.isOverloading(cmInt, amString));
		assertTrue(api.isOverloading(cmInt, cm));
		assertFalse(api.isOverloading(cmInt, cmInt));
		assertTrue(api.isOverloading(cmInt, cmDouble));

		assertTrue(api.isOverloading(cmDouble, im));
		assertTrue(api.isOverloading(cmDouble, amInt));
		assertTrue(api.isOverloading(cmDouble, amString));
		assertTrue(api.isOverloading(cmDouble, cm));
		assertTrue(api.isOverloading(cmDouble, cmInt));
		assertFalse(api.isOverloading(cmDouble, cmDouble));
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

		assertFalse(api.isOverloading(m1, m1));
		assertTrue(api.isOverloading(m1, m2));
		assertTrue(api.isOverloading(m2, m1));
		assertFalse(api.isOverloading(m2, m2));
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

		assertTrue(api.isOverloading(m1, m2));
		assertTrue(api.isOverloading(m1, m3));
		assertTrue(api.isOverloading(m2, m1));
		assertTrue(api.isOverloading(m2, m3));
		assertTrue(api.isOverloading(m3, m1));
		assertTrue(api.isOverloading(m3, m2));
	}
}
