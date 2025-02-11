package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertField;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
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

		assertThat(b.getDeclaredMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(2));

		assertThat(b.getSuperClass().getQualifiedName(), is(equalTo("A")));
		assertTrue(b.getSuperClass().getResolvedApiType().isPresent());
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

		assertThat(b.getDeclaredMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(2));
		assertThat(b.getSuperClass().getQualifiedName(), is(equalTo("A")));
		assertTrue(b.getSuperClass().getResolvedApiType().isPresent());
	}

	@Test
	void extends_unknown() {
		var api = buildAPI("""
			public class B extends Unknown {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertThat(b.getDeclaredMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(1));
		assertThat(b.getSuperClass().getQualifiedName(), is(equalTo("Unknown")));
		assertFalse(b.getSuperClass().getResolvedApiType().isPresent());
	}

	@Test
	void reference_unicity_fields() {
		var api = buildAPI("""
			public class C {
				public String f1;
				public String f2;
			}""");
		var c = assertClass(api, "C");
		var f1 = assertField(c, "f1");
		var f2 = assertField(c, "f2");

		assertThat(f1.getType(), sameInstance(f2.getType()));
	}

	@Test
	void reference_unicity_impl_api() {
		var api = buildAPI("""
			public interface I {}
			public class A implements I {}
			public class B implements I {}""");
		var a = assertClass(api, "A");
		var b = assertClass(api, "B");
		var implA = a.getImplementedInterfaces().getFirst();
		var implB = b.getImplementedInterfaces().getFirst();

		assertThat(implA, sameInstance(implB));
	}

	@Test
	void reference_unicity_super_jdk() {
		var api = buildAPI("""
			public class A extends java.lang.Thread {}
			public class B extends java.lang.Thread {}""");
		var a = assertClass(api, "A");
		var b = assertClass(api, "B");

		assertThat(a.getSuperClass(), sameInstance(b.getSuperClass()));
	}

	@Test
	void no_reference_unicity_across_APIs() {
		var api1 = buildAPI("""
			public class C {
				public String f1;
				public C f2;
			}""");
		var api2 = buildAPI("""
			public class C {
				public String f1;
				public C f2;
				public int f3;
			}""");

		var c1 = assertClass(api1, "C");
		var c2 = assertClass(api2, "C");
		var f11 = assertField(c1, "f1");
		var f12 = assertField(c1, "f2");
		var f21 = assertField(c2, "f1");
		var f22 = assertField(c2, "f2");

		assertThat(f11.getType(), equalTo(f21.getType()));
		assertThat(f11.getType(), not((sameInstance(f21.getType()))));
		assertThat(f12.getType(), equalTo(f22.getType()));
		assertThat(f12.getType(), not((sameInstance(f22.getType()))));

		assertThat(((TypeReference<?>) f12.getType()).getResolvedApiType().get(), equalTo(c1));
		assertThat(((TypeReference<?>) f22.getType()).getResolvedApiType().get(), equalTo(c2));
		assertThat(c1, not(equalTo(c2)));
	}
}
