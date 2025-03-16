package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertAnnotation;
import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertEnum;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeHierarchyExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_extends_class(ApiBuilder builder) {
		var api = builder.build("""
			class S {}
			class A extends S {}""");

		var s = assertClass(api, "S");
		var a = assertClass(api, "A");
		assertThat(a.getImplementedInterfaces(), is(empty()));
		assertThat(a.getSuperClass().getResolvedApiType().get(), is(equalTo(s)));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_implements_interfaces(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			interface J {}
			class A implements I, J {}""");

		var a = assertClass(api, "A");
		assertThat(a.getSuperClass(), is(equalTo(TypeReference.OBJECT)));
		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
		assertThat(a.getImplementedInterfaces().get(1).getQualifiedName(), is(equalTo("J")));
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
		assertThat(c.getImplementedInterfaces(), hasSize(1));
		assertThat(c.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("A")));
		assertTrue(c.getSuperClass().equals(TypeReference.OBJECT));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_implements_interface_and_extends_class(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			class S {}
			class A extends S implements I {}""");

		var a = assertClass(api, "A");
		assertThat(a.getSuperClass().getQualifiedName(), is(equalTo("S")));
		assertThat(a.getImplementedInterfaces(), hasSize(1));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_extends_interfaces(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			interface J {}
			interface A extends I, J {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
		assertThat(a.getImplementedInterfaces().get(1).getQualifiedName(), is(equalTo("J")));
	}

	// Surprisingly allowed
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_extends_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@interface I {}
			interface A extends I {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces(), hasSize(1));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void enum_implements_interface(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			enum E implements I {}""");

		var e = assertEnum(api, "E");
		assertEquals(TypeReference.ENUM, e.getSuperClass());
		assertThat(e.getImplementedInterfaces(), hasSize(1));
		assertThat(e.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void record_implements_interface(ApiBuilder builder) {
		var api = builder.build("""
			interface I {}
			record R() implements I {}""");

		var r = assertRecord(api, "R");
		assertEquals(TypeReference.RECORD, r.getSuperClass());
		assertThat(r.getImplementedInterfaces(), hasSize(1));
		assertThat(r.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
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

		assertThat(a.getSuperClass().getQualifiedName(), is(equalTo("E")));
		assertThat(a.getAllSuperClasses().toList(), hasSize(4));
		assertThat(a.getAllSuperClasses().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("java.lang.Object"), equalTo("C"), equalTo("D"), equalTo("E")));

		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getAllImplementedInterfaces().toList(), hasSize(6));
		assertThat(a.getAllImplementedInterfaces().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("I"), equalTo("J"), equalTo("K"), equalTo("L"), equalTo("M"), equalTo("N")));
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

		assertThat(a.getSuperClass().getQualifiedName(), is(equalTo("E")));
		assertThat(a.getAllSuperClasses().toList(), hasSize(4));
		assertThat(a.getAllSuperClasses().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("java.lang.Object"), equalTo("E"), equalTo("D"), equalTo("java.lang.Thread")));

		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getAllImplementedInterfaces().toList(), hasSize(5));
		assertThat(a.getAllImplementedInterfaces().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("M"), equalTo("N"), equalTo("java.lang.Comparable"), equalTo("java.lang.Runnable"), equalTo("java.lang.Cloneable")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void class_extends_generic_class(ApiBuilder builder) {
		var api = builder.build("""
		class GenericBase<T> {}
		class GenericChild extends GenericBase<String> {}
		""");

		assertClass(api, "GenericBase");
		var child = assertClass(api, "GenericChild");
		assertThat(child.getSuperClass().getQualifiedName(), is(equalTo("GenericBase")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_extends_generic_interface(ApiBuilder builder) {
		var api = builder.build("""
		interface Generic<T> {}
		interface Specialized extends Generic<String> {}
		""");

		var specialized = assertInterface(api, "Specialized");
		assertThat(specialized.getImplementedInterfaces(), hasSize(1));
		assertThat(specialized.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("Generic")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void diamond_inheritance_interface(ApiBuilder builder) {
		var api = builder.build("""
		interface A {}
		interface B extends A {}
		interface C extends A {}
		interface D extends B, C {}
		""");

		var d = assertInterface(api, "D");
		assertThat(d.getImplementedInterfaces().stream().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("B"), equalTo("C")));
		assertThat(d.getAllImplementedInterfaces().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("A"), equalTo("B"), equalTo("C")));
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
		}
		""");

		assertClass(api, "Outer");
		var inner = assertClass(api, "Outer$Inner");
		var nested = assertClass(api, "Outer$Nested");
		var innerInterface = assertInterface(api, "Outer$InnerInterface");
		var innerEnum = assertEnum(api, "Outer$InnerEnum");

		assertThat(inner.getSuperClass(), is(equalTo(TypeReference.OBJECT)));
		assertThat(nested.getSuperClass(), is(equalTo(TypeReference.OBJECT)));
		assertThat(innerInterface.getImplementedInterfaces(), is(empty()));
		assertThat(innerEnum.getSuperClass(), is(equalTo(TypeReference.ENUM)));
	}
}
