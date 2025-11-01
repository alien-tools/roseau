package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import static io.github.alien.roseau.utils.TestUtils.assertAnnotation;
import static io.github.alien.roseau.utils.TestUtils.assertAnnotationMethod;
import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertEnum;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
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
			.extracting(Annotation::actualAnnotation)
			.isEqualTo(new TypeReference<>(Deprecated.class.getCanonicalName()));
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

		assertThat(a.getTargets()).containsExactly(ElementType.TYPE);
		assertThat(c.getAnnotations())
			.singleElement()
			.extracting(Annotation::actualAnnotation)
			.isEqualTo(new TypeReference<>("A"));
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
			.extracting(Annotation::actualAnnotation)
			.isEqualTo(new TypeReference<>(Deprecated.class.getCanonicalName()));
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

		var a = assertAnnotation(api, "A");
		var c = assertClass(api, "C");
		var m = assertMethod(api, c, "m()");

		assertThat(a.getTargets()).containsExactly(ElementType.METHOD);
		assertThat(m.getAnnotations())
			.singleElement()
			.extracting(Annotation::actualAnnotation)
			.isEqualTo(new TypeReference<>("A"));
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
			.extracting(Annotation::actualAnnotation)
			.isEqualTo(new TypeReference<>(Deprecated.class.getCanonicalName()));
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

		var a = assertAnnotation(api, "A");
		var c = assertClass(api, "C");
		var f = assertField(api, c, "f");

		assertThat(a.getTargets()).containsExactly(ElementType.FIELD);
		assertThat(f.getAnnotations())
			.singleElement()
			.extracting(Annotation::actualAnnotation)
			.isEqualTo(new TypeReference<>("A"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void almighty_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Documented
			@java.lang.annotation.Inherited
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER,
				java.lang.annotation.ElementType.CONSTRUCTOR, java.lang.annotation.ElementType.LOCAL_VARIABLE,
				java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.PACKAGE,
				java.lang.annotation.ElementType.TYPE_PARAMETER, java.lang.annotation.ElementType.TYPE_USE,
				java.lang.annotation.ElementType.MODULE, java.lang.annotation.ElementType.RECORD_COMPONENT
			})
			@java.lang.annotation.Repeatable(Everything.Container.class)
			public @interface Everything {
				// ---------- Element types (all allowed kinds) ----------
				// Primitives
				boolean flag();
				byte b() default 1;
				short s();
				int i() default 3;
				long l();
				char c() default 'E';
				float f();
				double d() default 6.0d;
			
				// String
				String name() default "default";
			
				// Class and invocation of Class (bounded wildcard allowed)
				Class<?> type() default Object.class;
				Class<? extends Number> numberType() default Integer.class;
			
				// Enum type (uses nested enum below)
				Level level() default Level.GOOD;
			
				// Annotation-typed element (uses nested annotation below)
				Meta meta() default @Meta(key = "k", value = "v");
			
				// Arrays of any of the above
				int[] ports() default {80, 443};
				String[] tags() default {"alpha", "beta"};
				Class<?>[] components() default {String.class, Integer.class};
				Level[] levels() default {Level.GOOD, Level.EXCELLENT};
				Meta[] metas() default {@Meta(key = "a", value = "1"), @Meta(key = "b", value = "2")};
			
				// Type-use annotated return type is allowed; define a TYPE_USE meta-annotation below
				@TypeUseAnn String annotatedReturn() default "ok";
			
				// ---------- Constants (fields) ----------
				// These are implicitly public static final and must be constant expressions.
				int CONST_INT = 42;
				String CONST_STR = "CONST";
				long CONST_MASK = 0xFF_FF_FFL;
			
				// ---------- Nested member types (all are implicitly static) ----------
			
				// Nested enum for use by elements
				enum Level {BAD, INDIFFERENT, GOOD, EXCELLENT}
			
				// Nested annotation usable as an element type
				@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
				@java.lang.annotation.Target({java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.TYPE_USE})
				@interface Meta {
					String key();
					String value();
				}
			
				// Type-use-only annotation to demonstrate annotated element return type
				@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
				@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)
				@interface TypeUseAnn { }
			
				// Nested interface
				interface Helper {
					String help();
				}
			
				// Nested class
				class HelperImpl implements Helper {
					public String help() {
						return "help";
					}
				}
			
				// Nested record (Java 21 permits records as member types)
				record Pair(int left, int right) { }
			
				// You can also nest another annotation interface that serves as a utility
				@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
				@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.METHOD})
				@interface Marker { }
			
				// ---------- Repeatable container ----------
				// This is the containing annotation required by @Repeatable on Everything.
				@java.lang.annotation.Documented
				@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
				@java.lang.annotation.Target({
					java.lang.annotation.ElementType.TYPE, java.lang.annotation.ElementType.FIELD,
					java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.PARAMETER,
					java.lang.annotation.ElementType.CONSTRUCTOR, java.lang.annotation.ElementType.LOCAL_VARIABLE,
					java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.PACKAGE,
					java.lang.annotation.ElementType.TYPE_PARAMETER, java.lang.annotation.ElementType.TYPE_USE,
					java.lang.annotation.ElementType.MODULE, java.lang.annotation.ElementType.RECORD_COMPONENT
				})
				@java.lang.annotation.Inherited
				@interface Container {
					Everything[] value();
			
					// Any extra methods must have defaults
					String note() default "";
				}
			}""");

		// The annotation type itself
		var ann = assertAnnotation(api, "Everything");
		assertThat(ann.isPublic()).isTrue();
		assertThat(ann.hasAnnotation(TypeReference.ANNOTATION_DOCUMENTED)).isTrue();
		assertThat(ann.hasAnnotation(TypeReference.ANNOTATION_INHERITED)).isTrue();
		assertThat(ann.hasAnnotation(TypeReference.ANNOTATION_RETENTION)).isTrue();
		assertThat(ann.hasAnnotation(TypeReference.ANNOTATION_TARGET)).isTrue();
		assertThat(ann.hasAnnotation(TypeReference.ANNOTATION_REPEATABLE)).isTrue();

		assertThat(ann.getTargets()).containsExactlyInAnyOrder(
			ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
			ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PACKAGE, ElementType.PARAMETER,
			ElementType.MODULE, ElementType.RECORD_COMPONENT, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE
		);

		// We focus on the declared elements of the annotation type itself (methods/fields/nested types).

		// Declared element methods and their return types
		assertThat(ann.getAnnotationMethods()).hasSize(19);
		assertThat(assertAnnotationMethod(api, ann, "flag()").getType().getQualifiedName()).isEqualTo("boolean");
		assertThat(assertAnnotationMethod(api, ann, "flag()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, ann, "b()").getType().getQualifiedName()).isEqualTo("byte");
		assertThat(assertAnnotationMethod(api, ann, "b()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "s()").getType().getQualifiedName()).isEqualTo("short");
		assertThat(assertAnnotationMethod(api, ann, "s()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, ann, "i()").getType().getQualifiedName()).isEqualTo("int");
		assertThat(assertAnnotationMethod(api, ann, "i()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "l()").getType().getQualifiedName()).isEqualTo("long");
		assertThat(assertAnnotationMethod(api, ann, "l()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, ann, "c()").getType().getQualifiedName()).isEqualTo("char");
		assertThat(assertAnnotationMethod(api, ann, "c()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "f()").getType().getQualifiedName()).isEqualTo("float");
		assertThat(assertAnnotationMethod(api, ann, "f()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, ann, "d()").getType().getQualifiedName()).isEqualTo("double");
		assertThat(assertAnnotationMethod(api, ann, "d()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "name()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, ann, "name()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "type()").getType().getQualifiedName()).isEqualTo("java.lang.Class");
		assertThat(assertAnnotationMethod(api, ann, "type()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "numberType()").getType().getQualifiedName()).isEqualTo("java.lang.Class");
		assertThat(assertAnnotationMethod(api, ann, "numberType()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "level()").getType().getQualifiedName()).isEqualTo("Everything$Level");
		assertThat(assertAnnotationMethod(api, ann, "level()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "meta()").getType().getQualifiedName()).isEqualTo("Everything$Meta");
		assertThat(assertAnnotationMethod(api, ann, "meta()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "annotatedReturn()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, ann, "annotatedReturn()").hasDefault()).isTrue();
		// Arrays
		assertThat(assertAnnotationMethod(api, ann, "ports()").getType().getQualifiedName()).isEqualTo("int[]");
		assertThat(assertAnnotationMethod(api, ann, "ports()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "tags()").getType().getQualifiedName()).isEqualTo("java.lang.String[]");
		assertThat(assertAnnotationMethod(api, ann, "tags()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "components()").getType().getQualifiedName()).isEqualTo("java.lang.Class[]");
		assertThat(assertAnnotationMethod(api, ann, "components()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "levels()").getType().getQualifiedName()).isEqualTo("Everything$Level[]");
		assertThat(assertAnnotationMethod(api, ann, "levels()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, ann, "metas()").getType().getQualifiedName()).isEqualTo("Everything$Meta[]");
		assertThat(assertAnnotationMethod(api, ann, "metas()").hasDefault()).isTrue();

		// Declared constant fields
		assertThat(ann.getDeclaredFields()).hasSize(3);
		assertThat(assertField(api, ann, "CONST_INT").getType().getQualifiedName()).isEqualTo("int");
		assertThat(assertField(api, ann, "CONST_STR").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertField(api, ann, "CONST_MASK").getType().getQualifiedName()).isEqualTo("long");

		// Nested types: existence and basic properties
		assertEnum(api, "Everything$Level");
		var meta = assertAnnotation(api, "Everything$Meta");
		assertAnnotation(api, "Everything$TypeUseAnn");
		var helper = assertInterface(api, "Everything$Helper");
		var helperImpl = assertClass(api, "Everything$HelperImpl");
		assertRecord(api, "Everything$Pair");
		assertAnnotation(api, "Everything$Marker");
		var container = assertAnnotation(api, "Everything$Container");

		assertThat(assertMethod(api, helper, "help()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertMethod(api, helperImpl, "help()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, meta, "key()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, meta, "key()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, meta, "value()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, meta, "value()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, container, "value()").getType().getQualifiedName()).isEqualTo("Everything[]");
		assertThat(assertAnnotationMethod(api, container, "value()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, container, "note()").hasDefault()).isTrue();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void annotation_values_literals(ApiBuilder builder) {
		var api = builder.build("""
			@Deprecated(since = "1.0.0", forRemoval = true)
			public class A {}""");

		var a = assertClass(api, "A");
		var ann = a.getAnnotation(new TypeReference<>(Deprecated.class.getCanonicalName()));
		assertThat(ann).isPresent();
		assertThat(ann.get().values()).containsExactlyInAnyOrderEntriesOf(
			Map.of("since", "1.0.0",
				"forRemoval", "true"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void annotation_values_method(ApiBuilder builder) {
		var api = builder.build("""
			public @interface Ann { String value(); }
			public class A {
				@Deprecated(since = "1.0.0", forRemoval = true)
				@Ann("str")
				public void m() {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");
		var deprecated = m.getAnnotation(new TypeReference<>(Deprecated.class.getCanonicalName()));
		assertThat(deprecated).isPresent();
		assertThat(deprecated.get().values()).containsExactlyInAnyOrderEntriesOf(
			Map.of("since", "1.0.0",
				"forRemoval", "true"));
		var ann = m.getAnnotation(new TypeReference<>("Ann"));
		assertThat(ann).isPresent();
		assertThat(ann.get().values()).containsExactlyInAnyOrderEntriesOf(
			Map.of("value", "str"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void annotation_value_enum(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
			public @interface A {}""");

		var a = assertAnnotation(api, "A");
		var ann = a.getAnnotation(new TypeReference<>(Retention.class.getCanonicalName()));
		assertThat(ann).isPresent();
		assertThat(ann.get().values()).containsExactlyEntriesOf(
			Map.of("value", "java.lang.annotation.RetentionPolicy.RUNTIME"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void annotation_value_enum_inner(ApiBuilder builder) {
		var api = builder.build("""
			public @interface A {
				E value();
				enum E { A, B; }
			}
			@A(A.E.A) public class C {}""");

		var c = assertClass(api, "C");
		var ann = c.getAnnotation(new TypeReference<>("A"));
		assertThat(ann).isPresent();
		assertThat(ann.get().values()).containsExactlyEntriesOf(
			Map.of("value", "A$E.A"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void annotation_value_class(ApiBuilder builder) {
		var api = builder.build("""
			public @interface A {
				Class<?> value();
			}
			@A(C.class) public class C {}
			@A(java.lang.String.class) public class D {}""");

		var c = assertClass(api, "C");
		var d = assertClass(api, "D");
		var ca = c.getAnnotation(new TypeReference<>("A"));
		assertThat(ca).isPresent();
		assertThat(ca.get().values()).containsExactlyEntriesOf(
			Map.of("value", "C"));
		var da = d.getAnnotation(new TypeReference<>("A"));
		assertThat(da).isPresent();
		assertThat(da.get().values()).containsExactlyEntriesOf(
			Map.of("value", "java.lang.String"));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void source_annotations_are_ignored(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
			public @interface A1 {}
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			public @interface A2 {}
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
			public @interface A3 {}
			// RetentionPolicy.CLASS by default
			public @interface A4 {}
			@A1 @A2 @A3 @A4 public class C {}
			""");

		var c = assertClass(api, "C");
		assertThat(c.getAnnotations())
			.extracting(ann -> ann.actualAnnotation().getQualifiedName())
			.containsExactlyInAnyOrder("A2", "A3", "A4");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void syntethic_annotations(ApiBuilder builder) {
		var api = builder.build("""
			public record R(@Deprecated int x) {}""");

		var r = assertRecord(api, "R");
		assertThat(r.getDeclaredMethods().iterator().next().getAnnotations()).hasSize(1);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void array_class_values(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
			@java.lang.annotation.Target({
				java.lang.annotation.ElementType.ANNOTATION_TYPE,
				java.lang.annotation.ElementType.CONSTRUCTOR,
				java.lang.annotation.ElementType.FIELD,
				java.lang.annotation.ElementType.METHOD,
				java.lang.annotation.ElementType.TYPE
			})
			@Deprecated
			public @interface Beta {}""");

		var beta = assertAnnotation(api, "Beta");
		var ann = beta.getAnnotation(new TypeReference<>(Target.class.getCanonicalName())).orElseThrow();
		assertThat(beta.getTargets()).containsExactlyInAnyOrder(ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
			ElementType.FIELD, ElementType.METHOD, ElementType.TYPE);
		assertThat(ann.values()).containsEntry("value", "{}"); // We ignore those
	}
}
