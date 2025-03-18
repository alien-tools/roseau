package io.github.alien.roseau.extractors;

import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertNoMethod;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_methods(ApiBuilder builder) {
		var api = builder.build("""
			public interface I {
			  void m1();
			  default void m2() {}
			  static void m3() {}
			  private void m4() {}
			  private static void m5() {}
			}""");

		var i = assertInterface(api, "I");
		var m1 = assertMethod(i, "m1()");
		var m2 = assertMethod(i, "m2()");
		var m3 = assertMethod(i, "m3()");
		assertNoMethod(i, "m4()");
		assertNoMethod(i, "m5()");

		assertFalse(m1.isDefault());
		assertTrue(m1.isPublic());
		assertTrue(m2.isDefault());
		assertTrue(m2.isPublic());
		assertTrue(m3.isStatic());
		assertTrue(m3.isPublic());
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
	@EnumSource(value = ApiBuilderType.class, names = {"ASM"}, mode = EnumSource.Mode.EXCLUDE)
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_parameters(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T> {
				public void m1(int a, final String b, T c, int[] d) {}
				public void m2(int... a) {}
				public void m3(int a, int... b) {}
				public <U> void m4(U a) {}
				public <U extends CharSequence> void m5(U a, java.util.List<U> b, U[]... c) {}
			}""");

		var a = assertClass(api, "A");
		var m1 = assertMethod(a, "m1(int,java.lang.String,java.lang.Object,int[])");
		var m2 = assertMethod(a, "m2(int[])");
		var m3 = assertMethod(a, "m3(int,int[])");
		var m4 = assertMethod(a, "m4(java.lang.Object)");
		var m5 = assertMethod(a, "m5(java.lang.CharSequence,java.util.List,java.lang.CharSequence[][])");

		assertFalse(m1.isVarargs());
		assertTrue(m2.isVarargs());
		assertTrue(m3.isVarargs());
		assertFalse(m4.isVarargs());
		assertTrue(m5.isVarargs());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_thrown_exceptions(ApiBuilder builder) {

	}
}
