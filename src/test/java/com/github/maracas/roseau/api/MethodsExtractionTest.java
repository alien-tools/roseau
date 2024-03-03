package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
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
		var m1 = assertMethod(i, "m1");
		var m2 = assertMethod(i, "m2");

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
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

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
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

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
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

		assertFalse(m1.isNative());
		assertTrue(m2.isNative());
	}
}
