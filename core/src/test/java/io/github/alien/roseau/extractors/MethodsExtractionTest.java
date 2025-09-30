package io.github.alien.roseau.extractors;

import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertNoMethod;
import static io.github.alien.roseau.utils.TestUtils.buildSpoonAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_qualified_method(ApiBuilder builder) {
		var api = builder.build("""
			package pkg;
			public interface I {
				public void m();
			}""");

		var i = assertInterface(api, "pkg.I");
		var m = assertMethod(api, i, "m()");

		assertThat(m.getQualifiedName()).isEqualTo("pkg.I.m()");
		assertThat(m.getSimpleName()).isEqualTo("m");
	}

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
		var m1 = assertMethod(api, i, "m1()");
		var m2 = assertMethod(api, i, "m2()");
		var m3 = assertMethod(api, i, "m3()");
		assertNoMethod(api, i, "m4()");
		assertNoMethod(api, i, "m5()");

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
		var m1 = assertMethod(api, a, "m1()");
		var m2 = assertMethod(api, a, "m2()");

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
		var m1 = assertMethod(api, a, "m1()");
		var m2 = assertMethod(api, a, "m2()");

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
		var m1 = assertMethod(api, a, "m1()");
		var m2 = assertMethod(api, a, "m2()");

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
		var m1 = assertMethod(api, a, "m1(int,java.lang.String,java.lang.Object,int[])");
		var m2 = assertMethod(api, a, "m2(int[])");
		var m3 = assertMethod(api, a, "m3(int,int[])");
		var m4 = assertMethod(api, a, "m4(java.lang.Object)");
		var m5 = assertMethod(api, a, "m5(java.lang.CharSequence,java.util.List,java.lang.CharSequence[][])");

		assertFalse(m1.isVarargs());
		assertTrue(m2.isVarargs());
		assertTrue(m3.isVarargs());
		assertFalse(m4.isVarargs());
		assertTrue(m5.isVarargs());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void leaked_methods() {
		var v1 = buildSpoonAPI("""
			class A {
				public void m() {}
				protected void n() {}
				private void o() {}
			}
			public class B extends A {}
			public final class C extends B {}""");

		var a = assertClass(v1, "A");
		var am = assertMethod(v1, a, "m()");
		var an = assertMethod(v1, a, "n()");
		var b = assertClass(v1, "B");
		var c = assertClass(v1, "C");

		assertFalse(v1.isExported(a, am));
		assertFalse(v1.isExported(a, an));
		assertTrue(v1.isExported(b, am));
		assertTrue(v1.isExported(b, an));
		assertTrue(v1.isExported(c, am));
		assertFalse(v1.isExported(c, an));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_thrown_exceptions(ApiBuilder builder) {
		// FIXME
	}
}
