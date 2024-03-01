package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertAnnotation;
import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.assertMethod;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class AnnotationsExtractionTest {
	@Test
	void type_jdk_annotation() {
		var api = buildAPI("@Deprecated public class C {}");

		var c = assertClass(api, "C");

		assertThat(c.getAnnotations(), hasSize(1));
		assertThat(c.getAnnotations().getFirst().actualAnnotation().getQualifiedName(), is(equalTo("java.lang.Deprecated")));
	}

	@Test
	void type_custom_annotation() {
		var api = buildAPI("""
			@Target(ElementType.TYPE)
			public @interface A {}
			@A public class C {}""");

		var a = assertAnnotation(api, "A");
		var c = assertClass(api, "C");

		assertThat(c.getAnnotations(), hasSize(1));
		assertThat(c.getAnnotations().getFirst().actualAnnotation().getQualifiedName(), is(equalTo("A")));
	}

	@Test
	void method_jdk_annotation() {
		var api = buildAPI("""
			public class C {
				@Deprecated public void m() {}
			}""");

		var c = assertClass(api, "C");
		var m = assertMethod(c, "m");

		assertThat(m.getAnnotations(), hasSize(1));
		assertThat(m.getAnnotations().getFirst().actualAnnotation().getQualifiedName(), is(equalTo("java.lang.Deprecated")));
	}

	@Test
	void method_custom_annotation() {
		var api = buildAPI("""
			@Target(ElementType.METHOD)
			public @interface A {}
			public class C {
				@A public void m() {}
			}""");

		var c = assertClass(api, "C");
		var m = assertMethod(c, "m");

		assertThat(m.getAnnotations(), hasSize(1));
		assertThat(m.getAnnotations().getFirst().actualAnnotation().getQualifiedName(), is(equalTo("A")));
	}

	@Test
	void field_jdk_annotation() {
		var api = buildAPI("""
			public class C {
				@Deprecated public int f;
			}""");

		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		assertThat(f.getAnnotations(), hasSize(1));
		assertThat(f.getAnnotations().getFirst().actualAnnotation().getQualifiedName(), is(equalTo("java.lang.Deprecated")));
	}

	@Test
	void field_custom_annotation() {
		var api = buildAPI("""
			@Target(ElementType.FIELD)
			public @interface A {}
			public class C {
				@A public int f;
			}""");

		var c = assertClass(api, "C");
		var f = assertField(c, "f");

		assertThat(f.getAnnotations(), hasSize(1));
		assertThat(f.getAnnotations().getFirst().actualAnnotation().getQualifiedName(), is(equalTo("A")));
	}
}
