package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.utils.ApiBuilder;
import com.github.maracas.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void default_methods(ApiBuilder builder) {
		var api = builder.build("""
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void abstract_methods(ApiBuilder builder) {
		var api = builder.build("""
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

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"SOURCES"})
	void strictfp_methods(ApiBuilder builder) {
		var api = builder.build("""
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void native_methods(ApiBuilder builder) {
		var api = builder.build("""
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
}
