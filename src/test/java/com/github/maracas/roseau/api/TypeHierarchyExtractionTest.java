package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.reference.ITypeReference;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertAnnotation;
import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertEnum;
import static com.github.maracas.roseau.TestUtils.assertInterface;
import static com.github.maracas.roseau.TestUtils.assertRecord;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeHierarchyExtractionTest {
	@Test
	void class_extends_class() {
		var api = buildAPI("""
			class S {}
			class A extends S {}""");

		var s = assertClass(api, "S");
		var a = assertClass(api, "A");
		assertThat(a.getImplementedInterfaces(), is(empty()));
		assertThat(a.getSuperClass().get().getResolvedApiType().get(), is(equalTo(s)));
	}

	@Test
	void class_implements_interfaces() {
		var api = buildAPI("""
			interface I {}
			interface J {}
			class A implements I, J {}""");

		var a = assertClass(api, "A");
		assertTrue(a.getSuperClass().isEmpty());
		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
		assertThat(a.getImplementedInterfaces().get(1).getQualifiedName(), is(equalTo("J")));
	}

	// Surprisingly allowed
	@Test
	void class_extends_annotation() {
		var api = buildAPI("""
			@interface A {}
			class C implements A {
				@Override
				public Class<? extends Annotation> annotationType() {
					return null;
				}
			}""");

		assertAnnotation(api, "A");
		var c = assertClass(api, "C");
		assertThat(c.getImplementedInterfaces(), hasSize(1));
		assertThat(c.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("A")));
		assertTrue(c.getSuperClass().isEmpty());
	}

	@Test
	void class_implements_interface_and_extends_class() {
		var api = buildAPI("""
			interface I {}
			class S {}
			class A extends S implements I {}""");

		var a = assertClass(api, "A");
		assertThat(a.getSuperClass().get().getQualifiedName(), is(equalTo("S")));
		assertThat(a.getImplementedInterfaces(), hasSize(1));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void interface_extends_interfaces() {
		var api = buildAPI("""
			interface I {}
			interface J {}
			interface A extends I, J {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
		assertThat(a.getImplementedInterfaces().get(1).getQualifiedName(), is(equalTo("J")));
	}

	// Surprisingly allowed
	@Test
	void interface_extends_annotation() {
		var api = buildAPI("""
			@interface I {}
			interface A extends I {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getImplementedInterfaces(), hasSize(1));
		assertThat(a.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void enum_implements_interface() {
		var api = buildAPI("""
			interface I {}
			enum E implements I {}""");

		var e = assertEnum(api, "E");
		assertTrue(e.getSuperClass().isEmpty());
		assertThat(e.getImplementedInterfaces(), hasSize(1));
		assertThat(e.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void record_implements_interface() {
		var api = buildAPI("""
			interface I {}
			record R() implements I {}""");

		var r = assertRecord(api, "R");
		assertTrue(r.getSuperClass().isEmpty());
		assertThat(r.getImplementedInterfaces(), hasSize(1));
		assertThat(r.getImplementedInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void class_implements_hierarchy() {
		var api = buildAPI("""
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

		assertThat(a.getSuperClass().get().getQualifiedName(), is(equalTo("E")));
		assertThat(a.getAllSuperClasses(), hasSize(3));
		assertThat(a.getAllSuperClasses().stream().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("C"), equalTo("D"), equalTo("E")));

		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getAllImplementedInterfaces(), hasSize(6));
		assertThat(a.getAllImplementedInterfaces().stream().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("I"), equalTo("J"), equalTo("K"), equalTo("L"), equalTo("M"), equalTo("N")));
	}

	@Test
	void class_implements_jdk_hierarchy() {
		var api = buildAPI("""
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

		assertThat(a.getSuperClass().get().getQualifiedName(), is(equalTo("E")));
		assertThat(a.getAllSuperClasses(), hasSize(3));
		assertThat(a.getAllSuperClasses().stream().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("E"), equalTo("D"), equalTo("java.lang.Thread")));

		assertThat(a.getImplementedInterfaces(), hasSize(2));
		assertThat(a.getAllImplementedInterfaces(), hasSize(5));
		assertThat(a.getAllImplementedInterfaces().stream().map(ITypeReference::getQualifiedName).toList(),
			hasItems(equalTo("M"), equalTo("N"), equalTo("java.lang.Comparable"), equalTo("java.lang.Runnable"), equalTo("java.lang.Cloneable")));
	}
}
