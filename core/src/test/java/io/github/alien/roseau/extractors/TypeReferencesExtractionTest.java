package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static org.assertj.core.api.Assertions.assertThat;

class TypeReferencesExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_primitive(ApiBuilder builder) {
		var api = builder.build("public class C { public int f; }");
		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(f.getType()).isEqualTo(PrimitiveTypeReference.INT);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_jdk(ApiBuilder builder) {
		var api = builder.build("public class C { public String f; }");
		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(f.getType()).isEqualTo(TypeReference.STRING);
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
		var f = assertField(api, c, "f");

		assertThat(f.getType()).isEqualTo(new TypeReference<>("I"));
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
		var f = assertField(api, c, "f");

		assertThat(f.getType()).isEqualTo(new TypeReference<>("I"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_type_parameter(ApiBuilder builder) {
		var api = builder.build("public class C<T> { public T f; }");
		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(f.getType()).isEqualTo(new TypeParameterReference("T"));
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"ASM", "JDT"}, mode = EnumSource.Mode.EXCLUDE)
	void field_unknown(ApiBuilder builder) {
		var api = builder.build("public class C { public Unknown f; }");
		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(f.getType()).isEqualTo(new TypeReference<>("Unknown"));
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

		assertThat(b.getDeclaredMethods()).hasSize(1);
		assertThat(api.getExportedMethods(b)).hasSize(2 + 11);  // java.lang.Object's default
		assertThat(b.getSuperClass()).isEqualTo(new TypeReference<>("A"));
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

		assertThat(b.getDeclaredMethods()).hasSize(1);
		assertThat(api.getExportedMethods(b)).hasSize(2 + 11);  // java.lang.Object's default
		assertThat(b.getSuperClass()).isEqualTo(new TypeReference<>("A"));
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"SPOON"})
	void extends_unknown(ApiBuilder builder) {
		var api = builder.build("""
			public class B extends Unknown {
				public void m2() {}
			}""");
		var b = assertClass(api, "B");

		assertThat(b.getDeclaredMethods()).hasSize(1);
		assertThat(api.getExportedMethods(b)).hasSize(1);  // java.lang.Object's default
		assertThat(b.getSuperClass()).isEqualTo(new TypeReference<>("Unknown"));
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
		var f1 = assertField(api, c, "f1");
		var f2 = assertField(api, c, "f2");

		assertThat(f1.getType()).isSameAs(f2.getType());
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
		var implA = a.getImplementedInterfaces().iterator().next();
		var implB = b.getImplementedInterfaces().iterator().next();

		assertThat(implA).isSameAs(implB);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void reference_unicity_super_jdk(ApiBuilder builder) {
		var api = builder.build("""
			public class A extends java.lang.Thread {}
			public class B extends java.lang.Thread {}""");
		var a = assertClass(api, "A");
		var b = assertClass(api, "B");

		assertThat(a.getSuperClass()).isSameAs(b.getSuperClass());
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
		var f11 = assertField(api1, c1, "f1");
		var f12 = assertField(api1, c1, "f2");
		var f21 = assertField(api2, c2, "f1");
		var f22 = assertField(api2, c2, "f2");

		assertThat(f11.getType()).isEqualTo(f21.getType());
		assertThat(f11.getType()).isNotSameAs(f21.getType());
		assertThat(f12.getType()).isEqualTo(f22.getType());
		assertThat(f12.getType()).isNotSameAs(f22.getType());
		assertThat(c1).isNotEqualTo(c2);
	}
}
