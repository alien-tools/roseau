package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.AccessModifier;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertField;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsExtractionTest {
	@Test
	void default_methods() {
		var api = buildAPI("""
			public interface I {
			  void m1();
			  default void m2() {}
			}""");

		var i = assertInterface(api, "I");
		var m1 = assertMethod(i, "m1()");
		var m2 = assertMethod(i, "m2()");

		assertFalse(m1.isDefault());
		assertTrue(m2.isDefault());
	}

	@Test
	void abstract_methods() {
		var api = buildAPI("""
			public abstract class A {
			  public void m1() {}
			  public abstract void m2();
			}""");

		var a = assertClass(api, "A");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertFalse(m1.isAbstract());
		assertTrue(m2.isAbstract());
	}

	@Test
	void strictfp_methods() {
		var api = buildAPI("""
			public class A {
			  public void m1() {}
			  public strictfp void m2() {}
			}""");

		var a = assertClass(api, "A");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertFalse(m1.isStrictFp());
		assertTrue(m2.isStrictFp());
	}

	@Test
	void native_methods() {
		var api = buildAPI("""
			public class A {
			  public void m1() {}
			  public native void m2();
			}""");

		var a = assertClass(api, "A");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertFalse(m1.isNative());
		assertTrue(m2.isNative());
	}

	@Test
	void default_interface_visibilities() {
		var api = buildAPI("""
			public interface I {
				int f = 0;
			  void m();
			}""");

		var i = assertInterface(api, "I");
		var f = assertField(i, "f");
		var m = assertMethod(i, "m()");

		assertEquals(AccessModifier.PUBLIC, f.getVisibility());
		assertEquals(AccessModifier.PUBLIC, m.getVisibility());
		assertTrue(f.isPublic());
		assertTrue(m.isPublic());
	}

	@Test
	void default_class_visibilities() {
		var api = buildAPI("""
			public class C {
			  int f = 0;
			  void m() {}
			}""");

		var c = assertClass(api, "C");

		assertEquals(0, c.getAllFields().count());
		assertEquals(0, c.getAllMethods().count());
	}
}
