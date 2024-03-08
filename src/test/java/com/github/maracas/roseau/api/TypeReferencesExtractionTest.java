package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertField;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TypeReferencesExtractionTest {
	@Test
	void field_primitive() {
		var api = buildAPI("public class C { public int f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof PrimitiveTypeReference ref)
			assertThat(ref.qualifiedName(), is(equalTo("int")));
		else fail();
	}

	@Test
	void field_jdk() {
		var api = buildAPI("public class C { public String f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("java.lang.String")));
			assertTrue(ref.getResolvedApiType().isPresent());
		} else fail();
	}

	@Test
	void field_api() {
		var api = buildAPI("""
			public interface I {}
			public class C {
				public I f;
			}""");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("I")));
			assertTrue(ref.getResolvedApiType().isPresent());
		} else fail();
	}

	@Test
	void field_private_api() {
		var api = buildAPI("""
			interface I {}
			public class C {
				public I f;
			}""");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("I")));
			assertTrue(ref.getResolvedApiType().isPresent());
		} else fail();
	}

	@Test
	void field_type_parameter() {
		var api = buildAPI("public class C<T> { public T f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeParameterReference ref)
			assertThat(ref.getQualifiedName(), is(equalTo("T")));
		else fail();
	}

	@Test
	void field_unknown() {
		var api = buildAPI("public class C { public Unknown f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("Unknown")));
			assertFalse(ref.getResolvedApiType().isPresent());
		} else fail();
	}

	@Test
	void extends_api() {
		var api = buildAPI("""
			public class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertTrue(b.getSuperClass().isPresent());
		assertThat(b.getMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(2));

		if (b.getSuperClass().get() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("A")));
			assertTrue(ref.getResolvedApiType().isPresent());
		}
	}

	@Test
	void extends_private_api() {
		var api = buildAPI("""
			class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertTrue(b.getSuperClass().isPresent());
		assertThat(b.getMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(2));

		if (b.getSuperClass().get() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("A")));
			assertTrue(ref.getResolvedApiType().isPresent());
		}
	}

	@Test
	void extends_unknown() {
		var api = buildAPI("""
			public class B extends Unknown {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertTrue(b.getSuperClass().isPresent());
		assertThat(b.getMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(1));

		if (b.getSuperClass().get() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("Unknown")));
			assertFalse(ref.getResolvedApiType().isPresent());
		}
	}

	@Test
	void bounded_generic() {

	}
}
