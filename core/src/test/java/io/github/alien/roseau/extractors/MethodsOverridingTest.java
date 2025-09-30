package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;
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

		assertThat(api.getAllMethods(i))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("I.m()");

		assertThat(api.getAllMethods(j))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("J.m()");

		assertThat(api.getAllMethods(k))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("K.m()");

		assertThat(api.getAllMethods(a))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("A.m()");

		assertThat(api.getAllMethods(b))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("A.m()");

		assertThat(api.getAllMethods(c))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("C.m()");

		assertThat(api.getAllMethods(d))
			.hasSize(1 + 11) // java.lang.Object's methods
			.extracting(MethodDecl::getQualifiedName)
			.contains("K.m()");
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

		assertThat(a.getDeclaredMethods()).hasSize(2);
		assertThat(b.getDeclaredMethods()).hasSize(2);

		var ma = assertMethod(api, a, "m(java.lang.Object[])");
		var mb = assertMethod(api, b, "m(java.lang.Object[])");
		var na = assertMethod(api, a, "n(java.lang.Object[])");
		var nb = assertMethod(api, b, "n(java.lang.Object[])");

		assertFalse(api.isOverriding(ma, mb));
		assertFalse(api.isOverriding(na, nb));

		assertTrue(api.isOverriding(mb, ma));
		assertTrue(api.isOverriding(nb, na));
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
		var ma = assertMethod(api, a, "m(java.util.Collection)");
		var mb = assertMethod(api, b, "m(java.util.Collection)");

		assertTrue(api.isOverriding(mb, ma));
		assertFalse(api.isOverriding(ma, mb));
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
		assertThat(a.getDeclaredMethods()).hasSize(5);
		assertThat(b.getDeclaredMethods()).hasSize(5);

		var m1a = assertMethod(api, a, "m1()");
		var m2a = assertMethod(api, a, "m2()");
		var m3a = assertMethod(api, a, "m3()");
		var m4a = assertMethod(api, a, "m4()");
		var m5a = assertMethod(api, a, "m5()");

		var m1b = assertMethod(api, b, "m1()");
		var m2b = assertMethod(api, b, "m2()");
		var m3b = assertMethod(api, b, "m3()");
		var m4b = assertMethod(api, b, "m4()");
		var m5b = assertMethod(api, b, "m5()");

		assertThat(m1a.getType()).isEqualTo(PrimitiveTypeReference.INT);
		assertThat(m2a.getType()).isEqualTo(new TypeReference<>("java.lang.CharSequence"));
		assertThat(m3a.getType()).isEqualTo(new TypeParameterReference("T"));
		assertThat(m4a.getType()).isEqualTo(new TypeReference<>("java.lang.Number"));
		assertThat(m5a.getType()).isEqualTo(new TypeReference<>("java.util.List",
			List.of(new TypeReference<>("java.lang.Number"))));

		assertThat(m1b.getType()).isEqualTo(PrimitiveTypeReference.INT);
		assertThat(m2b.getType()).isEqualTo(TypeReference.STRING);
		assertThat(m3b.getType()).isEqualTo(new TypeParameterReference("U"));
		assertThat(m4b.getType()).isEqualTo(new TypeReference<>("java.lang.Integer"));
		assertThat(m5b.getType()).isEqualTo(new TypeReference<>("java.util.ArrayList",
			List.of(new TypeReference<>("java.lang.Number"))));

		assertTrue(api.isOverriding(m1b, m1a));
		assertTrue(api.isOverriding(m2b, m2a));
		assertTrue(api.isOverriding(m3b, m3a));
		assertTrue(api.isOverriding(m4b, m4a));
		assertTrue(api.isOverriding(m5b, m5a));
	}
}
