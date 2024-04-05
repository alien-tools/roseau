package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsOverloadingTest {
	@Test
	void overloading_methods() {
		var api = buildAPI("""
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

	@Test
	void overloading_methods_hierarchy() {
		var api = buildAPI("""
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
		assertThat(a.getAllMethods().toList(), hasSize(3));
		assertThat(c.getAllMethods().toList(), hasSize(4));

		var factory = api.getFactory();
		var intRef = factory.getTypeReferenceFactory().createPrimitiveTypeReference("int");
		var doubleRef = factory.getTypeReferenceFactory().createPrimitiveTypeReference("double");
		var stringRef = factory.getTypeReferenceFactory().createTypeReference("java.lang.String");

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

	@Test
	void overriding_multiple_sources() {
		var api = buildAPI("""
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

		assertThat(a.getAllMethods().toList(), hasSize(1));
		assertEquals("A.m", a.getAllMethods().toList().getFirst().getQualifiedName());

		assertThat(b.getAllMethods().toList(), hasSize(1));
		assertEquals("A.m", b.getAllMethods().toList().getFirst().getQualifiedName());

		assertThat(c.getAllMethods().toList(), hasSize(1));
		assertEquals("C.m", c.getAllMethods().toList().getFirst().getQualifiedName());

		assertThat(d.getAllMethods().toList(), hasSize(1));
		assertEquals("K.m", d.getAllMethods().toList().getFirst().getQualifiedName());
	}
}
