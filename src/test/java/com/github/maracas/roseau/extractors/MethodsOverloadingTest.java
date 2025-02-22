package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.utils.ApiBuilder;
import com.github.maracas.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
		assertThat(a.getDeclaredMethods(), hasSize(5));

		for (var m : a.getDeclaredMethods()) {
			for (var n : a.getDeclaredMethods()) {
				if (m == n) {
					assertFalse(m.isOverloading(n), m + " does overload " + n);
					assertTrue(m.isOverriding(n), m + " does not override " + n);
				} else {
					assertTrue(m.isOverloading(n), m + " does not overload " + n);
					assertFalse(m.isOverriding(n), m + " does override " + n);
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

		assertThat(i.getDeclaredMethods(), hasSize(1));
		assertThat(a.getDeclaredMethods(), hasSize(2));
		assertThat(c.getDeclaredMethods(), hasSize(3));

		assertThat(i.getAllMethods().toList(), hasSize(1));
		assertThat(a.getAllMethods().toList(), hasSize(3 + 11)); // java.lang.Object's methods
		assertThat(c.getAllMethods().toList(), hasSize(4 + 11)); // java.lang.Object's methods

		var im = assertMethod(i, "m()");
		var amInt = assertMethod(a, "m(int)");
		var amString = assertMethod(a, "m(java.lang.String)");
		var cm = assertMethod(c, "m()");
		var cmInt = assertMethod(c, "m(int)");
		var cmDouble = assertMethod(c, "m(double)");

		assertFalse(im.isOverloading(im));
		assertTrue(im.isOverloading(amInt));
		assertTrue(im.isOverloading(amString));
		assertFalse(im.isOverloading(cm));
		assertTrue(im.isOverloading(cmInt));
		assertTrue(im.isOverloading(cmDouble));

		assertTrue(amInt.isOverloading(im));
		assertFalse(amInt.isOverloading(amInt));
		assertTrue(amInt.isOverloading(amString));
		assertTrue(amInt.isOverloading(cm));
		assertFalse(amInt.isOverloading(cmInt));
		assertTrue(amInt.isOverloading(cmDouble));

		assertTrue(amString.isOverloading(im));
		assertTrue(amString.isOverloading(amInt));
		assertFalse(amString.isOverloading(amString));
		assertTrue(amString.isOverloading(cm));
		assertTrue(amString.isOverloading(cmInt));
		assertTrue(amString.isOverloading(cmDouble));

		assertFalse(cm.isOverloading(im));
		assertTrue(cm.isOverloading(amInt));
		assertTrue(cm.isOverloading(amString));
		assertFalse(cm.isOverloading(cm));
		assertTrue(cm.isOverloading(cmInt));
		assertTrue(cm.isOverloading(cmDouble));

		assertTrue(cmInt.isOverloading(im));
		assertFalse(cmInt.isOverloading(amInt));
		assertTrue(cmInt.isOverloading(amString));
		assertTrue(cmInt.isOverloading(cm));
		assertFalse(cmInt.isOverloading(cmInt));
		assertTrue(cmInt.isOverloading(cmDouble));

		assertTrue(cmDouble.isOverloading(im));
		assertTrue(cmDouble.isOverloading(amInt));
		assertTrue(cmDouble.isOverloading(amString));
		assertTrue(cmDouble.isOverloading(cm));
		assertTrue(cmDouble.isOverloading(cmInt));
		assertFalse(cmDouble.isOverloading(cmDouble));
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
		assertThat(a.getDeclaredMethods(), hasSize(2));

		var m1 = assertMethod(a, "m(int)");
		var m2 = assertMethod(a, "m(int[])");

		assertFalse(m1.isOverloading(m1));
		assertTrue(m1.isOverloading(m2));
		assertTrue(m2.isOverloading(m1));
		assertFalse(m2.isOverloading(m2));
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
		assertThat(a.getDeclaredMethods(), hasSize(3));

		var m1 = assertMethod(a, "m(java.lang.CharSequence)");
		var m2 = assertMethod(a, "m(java.lang.Number)");
		var m3 = assertMethod(a, "m(java.lang.Object)");

		assertTrue(m1.isOverloading(m2));
		assertTrue(m1.isOverloading(m3));
		assertTrue(m2.isOverloading(m1));
		assertTrue(m2.isOverloading(m3));
		assertTrue(m3.isOverloading(m1));
		assertTrue(m3.isOverloading(m2));
	}
}
