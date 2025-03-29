package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertAnnotation;
import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;

class AnnotationsExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_jdk_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@Deprecated public class C {}""");

		var c = assertClass(api, "C");

		assertThat(c.getAnnotations())
			.singleElement()
			.isEqualTo(new Annotation(new TypeReference<>("java.lang.Deprecated")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_custom_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
			@interface A {}
			@A public class C {}""");

		var a = assertAnnotation(api, "A");
		var c = assertClass(api, "C");

		assertThat(c.getAnnotations())
			.singleElement()
			.isEqualTo(new Annotation(new TypeReference<>("A")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_jdk_annotation(ApiBuilder builder) {
		var api = builder.build("""
			public class C {
				@Deprecated public void m() {}
			}""");

		var c = assertClass(api, "C");
		var m = assertMethod(api, c, "m()");

		assertThat(m.getAnnotations())
			.singleElement()
			.isEqualTo(new Annotation(new TypeReference<>("java.lang.Deprecated")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void method_custom_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
			@interface A {}
			public class C {
				@A public void m() {}
			}""");

		var c = assertClass(api, "C");
		var m = assertMethod(api, c, "m()");

		assertThat(m.getAnnotations())
			.singleElement()
			.isEqualTo(new Annotation(new TypeReference<>("A")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_jdk_annotation(ApiBuilder builder) {
		var api = builder.build("""
			public class C {
				@Deprecated public int f;
			}""");

		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(f.getAnnotations())
			.singleElement()
			.isEqualTo(new Annotation(new TypeReference<>("java.lang.Deprecated")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void field_custom_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
			@interface A {}
			public class C {
				@A public int f;
			}""");

		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(f.getAnnotations())
			.singleElement()
			.isEqualTo(new Annotation(new TypeReference<>("A")));
	}
}
