package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertAnnotation;
import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertEnum;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static org.assertj.core.api.Assertions.assertThat;

class TypeHierarchyExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_extends_class(ApiBuilder builder) {
		var api = builder.build("""
			class S {}
			class A extends S {}""");

		assertClass(api, "S");
		var a = assertClass(api, "A");
		assertThat(a.getImplementedInterfaces()).isEmpty();
		assertThat(a.getSuperClass())
			.isEqualTo(new TypeReference<>("S"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_implements_interfaces(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			interface J {}
			class A implements I, J {}""");

		var a = assertClass(api, "A");
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(a.getImplementedInterfaces())
			.containsOnly(new TypeReference<>("I"), new TypeReference<>("J"));
	}

	// Surprisingly allowed
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_extends_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@interface A {}
			class C implements A {
				@Override
				public Class<? extends java.lang.annotation.Annotation> annotationType() {
					return null;
				}
			}""");

		assertAnnotation(api, "A");
		var c = assertClass(api, "C");
		assertThat(c.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("A"));
		assertThat(c.getSuperClass()).isEqualTo(TypeReference.OBJECT);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_implements_interface_and_extends_class(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			class S {}
			class A extends S implements I {}""");

		var a = assertClass(api, "A");
		assertThat(a.getSuperClass()).isEqualTo(new TypeReference<>("S"));
		assertThat(a.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("I"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_extends_interfaces(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			interface J {}
			interface A extends I, J {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces())
			.containsExactlyInAnyOrder(new TypeReference<>("I"), new TypeReference<>("J"));
	}

	// Surprisingly allowed
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_extends_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@interface I {}
			interface A extends I {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("I"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void enum_implements_interface(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			enum E implements I {}""");

		var e = assertEnum(api, "E");
		assertThat(e.getSuperClass()).isEqualTo(TypeReference.ENUM);
		assertThat(e.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("I"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void record_implements_interface(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			record R() implements I {}""");

		var r = assertRecord(api, "R");
		assertThat(r.getSuperClass()).isEqualTo(TypeReference.RECORD);
		assertThat(r.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("I"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_implements_hierarchy(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			interface J {}
			interface K {}
			interface L {}
			interface M extends I {}
			interface N extends J, K {}
			class C {}
			class D extends C implements L {}
			class E extends D {}
			class A extends E implements M, N {}""");

		var a = assertClass(api, "A");

		assertThat(a.getSuperClass()).isEqualTo(new TypeReference<>("E"));
		assertThat(api.getAllSuperClasses(a))
			.containsOnly(new TypeReference<>("E"), new TypeReference<>("D"), new TypeReference<>("C"), TypeReference.OBJECT);
		assertThat(a.getImplementedInterfaces())
			.containsOnly(new TypeReference<>("M"), new TypeReference<>("N"));
		assertThat(api.getAllImplementedInterfaces(a))
			.containsOnly(new TypeReference<>("I"), new TypeReference<>("J"), new TypeReference<>("K"),
				new TypeReference<>("L"), new TypeReference<>("M"), new TypeReference<>("N"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_implements_jdk_hierarchy(ApiBuilder builder) {
		var api = builder.build("""
			interface M extends Runnable {}
			interface N extends Comparable<String>, Cloneable {}
			class D extends Thread implements M {}
			class E extends D {}
			class A extends E implements M, N {
				@Override
				public int compareTo(String o) {
					return 0;
				}
			}""");

		var a = assertClass(api, "A");

		assertThat(a.getSuperClass()).isEqualTo(new TypeReference<>("E"));
		assertThat(api.getAllSuperClasses(a))
			.containsOnly(new TypeReference<>("E"), new TypeReference<>("D"),
				new TypeReference<>("java.lang.Thread"), TypeReference.OBJECT);
		assertThat(api.getAllImplementedInterfaces(a))
			.containsOnly(new TypeReference<>("M"), new TypeReference<>("N"),
				new TypeReference<>("java.lang.Runnable"), new TypeReference<>("java.lang.Cloneable"),
				new TypeReference<>("java.lang.Comparable", List.of(TypeReference.STRING)));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_extends_generic_class(ApiBuilder builder) {
		var api = builder.build("""
			class GenericBase<T> {}
			class GenericChild extends GenericBase<String> {}""");

		assertClass(api, "GenericBase");
		var child = assertClass(api, "GenericChild");
		assertThat(child.getSuperClass()).isEqualTo(new TypeReference<>("GenericBase", List.of(TypeReference.STRING)));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_extends_generic_interface(ApiBuilder builder) {
		var api = builder.build("""
			interface Generic<T> {}
			interface Specialized extends Generic<String> {}""");

		var specialized = assertInterface(api, "Specialized");
		assertThat(specialized.getImplementedInterfaces())
			.singleElement()
			.isEqualTo(new TypeReference<>("Generic", List.of(TypeReference.STRING)));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void diamond_inheritance_interface(ApiBuilder builder) {
		var api = builder.build("""
			interface A {}
			interface B extends A {}
			interface C extends A {}
			interface D extends B, C {}""");

		var d = assertInterface(api, "D");
		assertThat(d.getImplementedInterfaces())
			.containsOnly(new TypeReference<>("B"), new TypeReference<>("C"));
		assertThat(api.getAllImplementedInterfaces(d))
			.containsOnly(new TypeReference<>("A"), new TypeReference<>("B"), new TypeReference<>("C"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_classes(ApiBuilder builder) {
		var api = builder.build("""
			class Outer {
				class Inner {}
				static class Nested {}
				interface InnerInterface {}
				enum InnerEnum {}
			}""");

		assertClass(api, "Outer");
		var inner = assertClass(api, "Outer$Inner");
		var nested = assertClass(api, "Outer$Nested");
		var innerInterface = assertInterface(api, "Outer$InnerInterface");
		var innerEnum = assertEnum(api, "Outer$InnerEnum");

		assertThat(inner.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(nested.getSuperClass()).isEqualTo(TypeReference.OBJECT);
		assertThat(innerInterface.getImplementedInterfaces()).isEmpty();
		assertThat(innerEnum.getSuperClass()).isEqualTo(TypeReference.ENUM);
	}
}
