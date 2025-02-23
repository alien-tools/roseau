package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.utils.ApiBuilder;
import com.github.maracas.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsOverridingTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void overriding_multiple_sources(ApiBuilder builder) {
		var api = builder.build("""
			public interface I { void m(); }
			public interface J { void m(); }
			public interface K extends I { default void m() {} }
			public class A { public void m() {} }
			public class B extends A implements J, K { }
			public class C extends A implements J, K { public void m() {} }
			public abstract class D implements K { }""");

		var i = assertInterface(api, "I");
		var j = assertInterface(api, "J");
		var k = assertInterface(api, "K");
		var a = assertClass(api, "A");
		var b = assertClass(api, "B");
		var c = assertClass(api, "C");
		var d = assertClass(api, "D");

		assertThat(i.getAllMethods().toList(), hasSize(1));
		assertEquals("I.m", i.getAllMethods().toList().getFirst().getQualifiedName());

		assertThat(j.getAllMethods().toList(), hasSize(1));
		assertEquals("J.m", j.getAllMethods().toList().getFirst().getQualifiedName());

		assertThat(k.getAllMethods().toList(), hasSize(1));
		assertEquals("K.m", k.getAllMethods().toList().getFirst().getQualifiedName());

		assertThat(a.getAllMethods().toList(), hasSize(1 + 11)); // java.lang.Object's defaults
		assertThat(a.getAllMethods().map(MethodDecl::getQualifiedName).toList(), hasItem("A.m"));

		assertThat(b.getAllMethods().toList(), hasSize(1 + 11)); // java.lang.Object's defaults
		assertThat(b.getAllMethods().map(MethodDecl::getQualifiedName).toList(), hasItem("A.m"));

		assertThat(c.getAllMethods().toList(), hasSize(1 + 11)); // java.lang.Object's defaults
		assertThat(c.getAllMethods().map(MethodDecl::getQualifiedName).toList(), hasItem("C.m"));

		assertThat(d.getAllMethods().toList(), hasSize(1 + 11)); // java.lang.Object's defaults
		assertThat(d.getAllMethods().map(MethodDecl::getQualifiedName).toList(), hasItem("K.m"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void overriding_varargs_hierarchy(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m(Object... a) {}
				public void n(Object[] a) {}
			}
			public class B extends A {
				@Override public void m(Object[] a) {}
				@Override public void n(Object... a) {}
			}""");

		var a = assertClass(api, "A");
		var b = assertClass(api, "B");

		assertThat(a.getDeclaredMethods(), hasSize(2));
		assertThat(b.getDeclaredMethods(), hasSize(2));

		var ma = assertMethod(a, "m(java.lang.Object[])");
		var mb = assertMethod(b, "m(java.lang.Object[])");
		var na = assertMethod(a, "n(java.lang.Object[])");
		var nb = assertMethod(b, "n(java.lang.Object[])");

		assertFalse(ma.isOverriding(mb));
		assertFalse(na.isOverriding(nb));

		assertTrue(mb.isOverriding(ma));
		assertTrue(nb.isOverriding(na));
	}

	// Example §8.4.2-1
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void override_generics(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public <T> java.util.List<T> m(java.util.Collection<T> p) { return null; }
			}
			public class B extends A {
				public java.util.List m(java.util.Collection p) { return null; }
			}""");

		var a = assertClass(api, "A");
		var b = assertClass(api, "B");
		var ma = assertMethod(a, "m(java.util.Collection)");
		var mb = assertMethod(b, "m(java.util.Collection)");

		assertTrue(mb.isOverriding(ma));
		assertFalse(ma.isOverriding(mb));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void covariant_overriding(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T> {
				public int m1() { return 0; } // Invariant
				public CharSequence m2() { return ""; }
				public T m3() { return null; }
				public Number m4() { return 0; }
				public java.util.List<Number> m5() { return null; }
			}
			public class B<T, U extends T> extends A<T> {
				@Override public int m1() { return 0; }
				@Override public String m2() { return ""; }
				@Override public U m3() { return null; }
				@Override public Integer m4() { return 0; }
				@Override public java.util.ArrayList<Number> m5() { return null; }
			}""");

		var a = assertClass(api, "A");
		var b = assertClass(api, "B");
		assertThat(a.getDeclaredMethods(), hasSize(5));
		assertThat(b.getDeclaredMethods(), hasSize(5));

		var m1a = assertMethod(a, "m1()");
		var m2a = assertMethod(a, "m2()");
		var m3a = assertMethod(a, "m3()");
		var m4a = assertMethod(a, "m4()");
		var m5a = assertMethod(a, "m5()");

		var m1b = assertMethod(b, "m1()");
		var m2b = assertMethod(b, "m2()");
		var m3b = assertMethod(b, "m3()");
		var m4b = assertMethod(b, "m4()");
		var m5b = assertMethod(b, "m5()");

		assertTrue(m1b.isOverriding(m1a));
		assertTrue(m2b.isOverriding(m2a));
		assertTrue(m3b.isOverriding(m3a));
		assertTrue(m4b.isOverriding(m4a));
		assertTrue(m5b.isOverriding(m5a));
	}
}
