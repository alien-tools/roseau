package com.github.maracas.roseau.api;

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
		assertThat(a.getSuperInterfaces(), is(empty()));
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
		assertThat(a.getSuperInterfaces(), hasSize(2));
		assertThat(a.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
		assertThat(a.getSuperInterfaces().get(1).getQualifiedName(), is(equalTo("J")));
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
		assertThat(c.getSuperInterfaces(), hasSize(1));
		assertThat(c.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("A")));
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
		assertThat(a.getSuperInterfaces(), hasSize(1));
		assertThat(a.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void interface_extends_interfaces() {
		var api = buildAPI("""
			interface I {}
			interface J {}
			interface A extends I, J {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getSuperInterfaces(), hasSize(2));
		assertThat(a.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
		assertThat(a.getSuperInterfaces().get(1).getQualifiedName(), is(equalTo("J")));
	}

	// Surprisingly allowed
	@Test
	void interface_extends_annotation() {
		var api = buildAPI("""
			@interface I {}
			interface A extends I {}""");

		var a = assertInterface(api, "A");
		assertThat(a.getSuperInterfaces(), hasSize(1));
		assertThat(a.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void enum_implements_interface() {
		var api = buildAPI("""
			interface I {}
			enum E implements I {}""");

		var e = assertEnum(api, "E");
		assertTrue(e.getSuperClass().isEmpty());
		assertThat(e.getSuperInterfaces(), hasSize(1));
		assertThat(e.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void record_implements_interface() {
		var api = buildAPI("""
			interface I {}
			record R() implements I {}""");

		var r = assertRecord(api, "R");
		assertTrue(r.getSuperClass().isEmpty());
		assertThat(r.getSuperInterfaces(), hasSize(1));
		assertThat(r.getSuperInterfaces().getFirst().getQualifiedName(), is(equalTo("I")));
	}
}
