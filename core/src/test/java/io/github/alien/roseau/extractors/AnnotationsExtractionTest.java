package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
			.isEqualTo(new Annotation(new TypeReference<>("java.lang.Deprecated")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_custom_annotation(ApiBuilder builder) {
		var api = builder.build("""
			@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
			@interface A {}
			@A public class C {}""");

		assertAnnotation(api, "A");
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
		var everything = assertAnnotation(api, "Everything");

		// We focus on the declared elements of the annotation type itself (methods/fields/nested types).

		// Declared element methods and their return types
		assertThat(everything.getAnnotationMethods()).hasSize(19);
		assertThat(assertAnnotationMethod(api, everything, "flag()").getType().getQualifiedName()).isEqualTo("boolean");
		assertThat(assertAnnotationMethod(api, everything, "flag()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, everything, "b()").getType().getQualifiedName()).isEqualTo("byte");
		assertThat(assertAnnotationMethod(api, everything, "b()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "s()").getType().getQualifiedName()).isEqualTo("short");
		assertThat(assertAnnotationMethod(api, everything, "s()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, everything, "i()").getType().getQualifiedName()).isEqualTo("int");
		assertThat(assertAnnotationMethod(api, everything, "i()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "l()").getType().getQualifiedName()).isEqualTo("long");
		assertThat(assertAnnotationMethod(api, everything, "l()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, everything, "c()").getType().getQualifiedName()).isEqualTo("char");
		assertThat(assertAnnotationMethod(api, everything, "c()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "f()").getType().getQualifiedName()).isEqualTo("float");
		assertThat(assertAnnotationMethod(api, everything, "f()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, everything, "d()").getType().getQualifiedName()).isEqualTo("double");
		assertThat(assertAnnotationMethod(api, everything, "d()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "name()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, everything, "name()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "type()").getType().getQualifiedName()).isEqualTo("java.lang.Class");
		assertThat(assertAnnotationMethod(api, everything, "type()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "numberType()").getType().getQualifiedName()).isEqualTo("java.lang.Class");
		assertThat(assertAnnotationMethod(api, everything, "numberType()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "level()").getType().getQualifiedName()).isEqualTo("Everything$Level");
		assertThat(assertAnnotationMethod(api, everything, "level()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "meta()").getType().getQualifiedName()).isEqualTo("Everything$Meta");
		assertThat(assertAnnotationMethod(api, everything, "meta()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "annotatedReturn()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, everything, "annotatedReturn()").hasDefault()).isTrue();
		// Arrays
		assertThat(assertAnnotationMethod(api, everything, "ports()").getType().getQualifiedName()).isEqualTo("int[]");
		assertThat(assertAnnotationMethod(api, everything, "ports()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "tags()").getType().getQualifiedName()).isEqualTo("java.lang.String[]");
		assertThat(assertAnnotationMethod(api, everything, "tags()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "components()").getType().getQualifiedName()).isEqualTo("java.lang.Class[]");
		assertThat(assertAnnotationMethod(api, everything, "components()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "levels()").getType().getQualifiedName()).isEqualTo("Everything$Level[]");
		assertThat(assertAnnotationMethod(api, everything, "levels()").hasDefault()).isTrue();
		assertThat(assertAnnotationMethod(api, everything, "metas()").getType().getQualifiedName()).isEqualTo("Everything$Meta[]");
		assertThat(assertAnnotationMethod(api, everything, "metas()").hasDefault()).isTrue();

		// Declared constant fields
		assertThat(everything.getDeclaredFields()).hasSize(3);
		assertThat(assertField(api, everything, "CONST_INT").getType().getQualifiedName()).isEqualTo("int");
		assertThat(assertField(api, everything, "CONST_STR").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertField(api, everything, "CONST_MASK").getType().getQualifiedName()).isEqualTo("long");

		// Nested types: existence and basic properties
		assertEnum(api, "Everything$Level");
		var meta = assertAnnotation(api, "Everything$Meta");
		var typeUseAnn = assertAnnotation(api, "Everything$TypeUseAnn");
		var helper = assertInterface(api, "Everything$Helper");
		var helperImpl = assertClass(api, "Everything$HelperImpl");
		// Pair may be extracted differently by some backends; prefer record if available, otherwise class
		var pairRecordPresent = api.getLibraryTypes().findType("Everything$Pair").map(t -> t.isRecord()).orElse(false);
		if (pairRecordPresent) {
			assertRecord(api, "Everything$Pair");
		} else {
			assertClass(api, "Everything$Pair");
		}
		var marker = assertAnnotation(api, "Everything$Marker");
		var container = assertAnnotation(api, "Everything$Container");

		// Check some members of nested types
		assertThat(assertMethod(api, helper, "help()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertMethod(api, helperImpl, "help()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		// Meta annotation methods
		assertThat(assertAnnotationMethod(api, meta, "key()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, meta, "key()").hasDefault()).isFalse();
		assertThat(assertAnnotationMethod(api, meta, "value()").getType().getQualifiedName()).isEqualTo("java.lang.String");
		assertThat(assertAnnotationMethod(api, meta, "value()").hasDefault()).isFalse();
		// Container must have Everything[] value()
		assertThat(assertAnnotationMethod(api, container, "value()").getType().getQualifiedName()).isEqualTo("Everything[]");
		assertThat(assertAnnotationMethod(api, container, "value()").hasDefault()).isFalse();
		// And any extra methods must have defaults (see note())
		assertThat(assertAnnotationMethod(api, container, "note()").hasDefault()).isTrue();
	}
}
