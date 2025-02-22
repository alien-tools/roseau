package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.utils.ApiBuilder;
import com.github.maracas.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertField;
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
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_primitive(ApiBuilder builder) {
		var api = builder.build("public class C { public int f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof PrimitiveTypeReference ref)
			assertThat(ref.qualifiedName(), is(equalTo("int")));
		else fail();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_jdk(ApiBuilder builder) {
		var api = builder.build("public class C { public String f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("java.lang.String")));
			assertTrue(ref.getResolvedApiType().isPresent());
		} else fail();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_api(ApiBuilder builder) {
		var api = builder.build("""
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_private_api(ApiBuilder builder) {
		var api = builder.build("""
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

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_type_parameter(ApiBuilder builder) {
		var api = builder.build("public class C<T> { public T f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeParameterReference ref)
			assertThat(ref.getQualifiedName(), is(equalTo("T")));
		else fail();
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"ASM", "JDT"}, mode = EnumSource.Mode.EXCLUDE)
	void field_unknown(ApiBuilder builder) {
		var api = builder.build("public class C { public Unknown f; }");
		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		if (f.getType() instanceof TypeReference<?> ref) {
			assertThat(ref.getQualifiedName(), is(equalTo("Unknown")));
			assertFalse(ref.getResolvedApiType().isPresent());
		} else fail();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void extends_api(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertThat(b.getDeclaredMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(2 + 11));  // java.lang.Object's default

		assertThat(b.getSuperClass().getQualifiedName(), is(equalTo("A")));
		assertTrue(b.getSuperClass().getResolvedApiType().isPresent());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void extends_private_api(ApiBuilder builder) {
		var api = builder.build("""
			class A {
				public void m1() {}
			}
			public class B extends A {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertThat(b.getDeclaredMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(2 + 11)); // java.Lang.Object's defaults
		assertThat(b.getSuperClass().getQualifiedName(), is(equalTo("A")));
		assertTrue(b.getSuperClass().getResolvedApiType().isPresent());
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"SPOON"})
	void extends_unknown(ApiBuilder builder) {
		var api = builder.build("""
			public class B extends Unknown {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertThat(b.getDeclaredMethods(), hasSize(1));
		assertThat(b.getAllMethods().toList(), hasSize(1));
		assertThat(b.getSuperClass().getQualifiedName(), is(equalTo("Unknown")));
		assertFalse(b.getSuperClass().getResolvedApiType().isPresent());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void reference_unicity_fields(ApiBuilder builder) {
		var api = builder.build("""
			public class C {
				public String f1;
				public String f2;
			}""");
		var c = assertClass(api, "C");
		var f1 = assertField(c, "f1");
		var f2 = assertField(c, "f2");

		assertThat(f1.getType(), sameInstance(f2.getType()));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void reference_unicity_impl_api(ApiBuilder builder) {
		var api = builder.build("""
			public interface I {}
			public class A implements I {}
			public class B implements I {}""");
		var a = assertClass(api, "A");
		var b = assertClass(api, "B");
		var implA = a.getImplementedInterfaces().getFirst();
		var implB = b.getImplementedInterfaces().getFirst();

		assertThat(implA, sameInstance(implB));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void reference_unicity_super_jdk(ApiBuilder builder) {
		var api = builder.build("""
			public class A extends java.lang.Thread {}
			public class B extends java.lang.Thread {}""");
		var a = assertClass(api, "A");
		var b = assertClass(api, "B");

		assertThat(a.getSuperClass(), sameInstance(b.getSuperClass()));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void no_reference_unicity_across_APIs(ApiBuilder builder) {
		var api1 = builder.build("""
			public class C {
				public String f1;
				public C f2;
			}""");
		var api2 = builder.build("""
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
