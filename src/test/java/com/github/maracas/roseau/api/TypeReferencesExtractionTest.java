package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.assertInterface;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class TypeReferencesExtractionTest {
	@Test
	void fields_types() {
		var api = buildAPI("""
			public class A<T> {
				public int primitive;
				public String jdk;
				public I api;
				public T typeParameter;
				public X unknown;
				public int[] primitiveArray;
				public String[] jdkArray;
				public I[] apiArray;
				public T[] typeParameterArray;
				public List<String> jdkGeneric;
				public List<I> apiGeneric;
				public List<T> typeParameterGeneric;
			}""");
	}

	@Test
	void jdk_type() {
		var api = buildAPI("""
			class A extends String {}""");

		var a = assertClass(api, "A");
		var sup = a.getSuperClass();

		var chars = a.getAllMethods()
			.filter(m -> "java.lang.String.chars".equals(m.getQualifiedName()))
			.findFirst()
			.get();
	}

	@Test
	void unknown_type() {
		var api = buildAPI("""
			class A extends Unknown {}""");

		var a = assertClass(api, "A");
		var sup = a.getSuperClass();
	}

	@Test
	void primitive_type_reference() {
		var api = buildAPI("public class A { public int f; }");
		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var t = f.getType();

//		assertTrue(t.isPrimitive());
		assertThat(t.getQualifiedName(), is(equalTo("int")));
	}

	@Test
	void api_type_reference() {
		var api = buildAPI("""
			abstract class B extends Thread {}
			public class A {
				public B f;
			}""");
		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var t = f.getType();

//		assertTrue(t.isClass());
//		assertTrue(t.isPackagePrivate());
//		assertTrue(t.isAbstract());
//		assertThat(t.getAllImplementedInterfaces(), hasSize(1));
//		assertThat(t.getAllImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("java.lang.Runnable")));
//		assertThat(t.getFormalTypeParameters(), is(empty()));
//		assertThat(t.getAllMethods(), is(not(empty())));
//		assertThat(t.getAllFields(), is(not(empty())));
	}

	@Test
	void jdk_type_reference() {
		var api = buildAPI("""
			public class A {
				public Runnable f;
			}""");
		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var t = f.getType();

//		assertTrue(t.isInterface());
//		assertThat(t.getAllImplementedInterfaces(), is(empty()));
//		assertThat(t.getFormalTypeParameters(), is(empty()));
//		assertThat(t.getAllMethods(), hasSize(1));
//		assertThat(t.getMethods().getFirst().getSimpleName(), is(equalTo("run")));
//		assertThat(t.getAllFields(), is(empty()));
	}

	@Test
	void type_parameter_reference() {
		var api = buildAPI("""
			public class A<T> {
				public T f;
			}""");
		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var t = f.getType();
	}

	@Test
	void type_parameter_reference_with_bounds() {
		var api = buildAPI("""
			interface I { void m(); }
			public class A<T extends String & I> {
				public T f;
			}""");
		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var t = f.getType();

//		assertFalse(t.isClass());
//		assertFalse(t.isInterface());
//		assertFalse(t.isEnum());
//		assertFalse(t.isRecord());
//		assertFalse(t.isAnnotation());
//		assertFalse(t.isExported());
//		assertFalse(t.isPackagePrivate());
//		assertFalse(t.isPrimitive());
//		assertFalse(t.isStatic());
//		assertFalse(t.isAbstract());
//		assertFalse(t.isFinal());
//		assertFalse(t.isSealed());
//		assertFalse(t.isNested());
//		assertFalse(t.isCheckedException());
//		assertFalse(t.isEffectivelyFinal());
//		assertThat(t.getAllImplementedInterfaces(), is(empty()));
//		assertThat(t.getFormalTypeParameters(), is(empty()));
//		assertThat(t.getAllMethods(), is(empty()));
//		assertThat(t.getAllFields(), is(empty()));
	}

	@Test
	void unknown_type_reference() {
		var api = buildAPI("""
			public class A {
				public UnknownType f;
			}""");
		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var t = f.getType();

//		assertFalse(t.isClass());
//		assertFalse(t.isInterface());
//		assertFalse(t.isEnum());
//		assertFalse(t.isRecord());
//		assertFalse(t.isAnnotation());
//		assertFalse(t.isExported());
//		assertFalse(t.isPackagePrivate());
//		assertFalse(t.isPrimitive());
//		assertFalse(t.isStatic());
//		assertFalse(t.isAbstract());
//		assertFalse(t.isFinal());
//		assertFalse(t.isSealed());
//		assertFalse(t.isNested());
//		assertFalse(t.isCheckedException());
//		assertFalse(t.isEffectivelyFinal());
//		assertThat(t.getAllImplementedInterfaces(), is(empty()));
//		assertThat(t.getFormalTypeParameters(), is(empty()));
//		assertThat(t.getAllMethods(), is(empty()));
//		assertThat(t.getAllFields(), is(empty()));
	}

	@Test
	void api_references() {
		var api = buildAPI("""
			interface J {}
			public interface I extends J {
				J j = null;
				J foo(J j);
				List<J> bar(List<J> j);
			}""");

		var i = assertInterface(api, "I");
	}
}
