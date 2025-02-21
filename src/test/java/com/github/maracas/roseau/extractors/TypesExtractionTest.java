package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.utils.ApiBuilder;
import com.github.maracas.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.github.maracas.roseau.utils.TestUtils.assertAnnotation;
import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertEnum;
import static com.github.maracas.roseau.utils.TestUtils.assertInterface;
import static com.github.maracas.roseau.utils.TestUtils.assertRecord;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypesExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_class(ApiBuilder builder) {
		var api = builder.build("class A {}");

		var a = assertClass(api, "A");

		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_class(ApiBuilder builder) {
		var api = builder.build("public class A {}");

		var a = assertClass(api, "A");

		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_interface(ApiBuilder builder) {
		var api = builder.build("interface A {}");

		var a = assertInterface(api, "A");

		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_interface(ApiBuilder builder) {
		var api = builder.build("public interface A {}");

		var a = assertInterface(api, "A");

		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_record(ApiBuilder builder) {
		var api = builder.build("record A() {}");

		var a = assertRecord(api, "A");

		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_record(ApiBuilder builder) {
		var api = builder.build("public record A() {}");

		var a = assertRecord(api, "A");

		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_enum(ApiBuilder builder) {
		var api = builder.build("enum A {}");

		var a = assertEnum(api, "A");

		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_enum(ApiBuilder builder) {
		var api = builder.build("public enum A {}");

		var a = assertEnum(api, "A");

		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_annotation(ApiBuilder builder) {
		var api = builder.build("@interface A {}");

		var a = assertAnnotation(api, "A");

		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_annotation(ApiBuilder builder) {
		var api = builder.build("public @interface A {}");

		var a = assertAnnotation(api, "A");

		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void local_class_is_ignored(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {
					class Local {}
				}
			}""");

		assertClass(api, "A");
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void anonymous_class_is_ignored(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {
					Runnable r = () -> {};
				}
			}""");

		assertClass(api, "A");
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void abstract_classes(ApiBuilder builder) {
		var api = builder.build("""
        public class A {}
        public abstract class B {}""");

		var a = assertClass(api, "A");
		assertFalse(a.isAbstract());

		var b = assertClass(api, "B");
		assertTrue(b.isAbstract());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void final_public_classes(ApiBuilder builder) {
		var api = builder.build("""
        public class A {}
        public final class B {}""");

		var a = assertClass(api, "A");
		assertFalse(a.isFinal());

		var b = assertClass(api, "B");
		assertTrue(b.isFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void sealed_classes(ApiBuilder builder) {
		var api = builder.build("""
			class A {}
			sealed class B permits C, D, E {}
			sealed class C extends B permits F {}
			final class D extends B {}
			non-sealed class E extends B {}
			final class F extends C {}""");

		var a = assertClass(api, "A");
		assertFalse(a.isFinal());
		assertFalse(a.isSealed());
		assertTrue(a.isEffectivelyFinal());

		var b = assertClass(api, "B");
		assertFalse(b.isFinal());
		assertTrue(b.isSealed());
		assertTrue(b.isEffectivelyFinal());

		var c = assertClass(api, "C");
		assertFalse(c.isFinal());
		assertTrue(c.isSealed());
		assertTrue(c.isEffectivelyFinal());

		var d = assertClass(api, "D");
		assertTrue(d.isFinal());
		assertFalse(d.isSealed());
		assertTrue(d.isEffectivelyFinal());

		var e = assertClass(api, "E");
		assertFalse(e.isFinal());
		assertFalse(e.isSealed());
		assertTrue(e.isEffectivelyFinal());

		var f = assertClass(api, "F");
		assertTrue(f.isFinal());
		assertFalse(f.isSealed());
		assertTrue(f.isEffectivelyFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void sealed_interfaces(ApiBuilder builder) {
		var api = builder.build("""
			interface A {}
			sealed interface B permits C, D {}
			sealed interface C extends B permits E {}
			non-sealed interface D extends B {}
			final class E implements C {}""");

		var a = assertInterface(api, "A");
		assertFalse(a.isFinal());
		assertFalse(a.isSealed());
		assertFalse(a.isEffectivelyFinal());

		var b = assertInterface(api, "B");
		assertFalse(b.isFinal());
		assertTrue(b.isSealed());
		assertTrue(b.isEffectivelyFinal());

		var c = assertInterface(api, "C");
		assertFalse(c.isFinal());
		assertTrue(c.isSealed());
		assertTrue(c.isEffectivelyFinal());

		var d = assertInterface(api, "D");
		assertFalse(d.isFinal());
		assertFalse(d.isSealed());
		assertFalse(d.isEffectivelyFinal());

		var e = assertClass(api, "E");
		assertTrue(e.isFinal());
		assertFalse(e.isSealed());
		assertTrue(e.isEffectivelyFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void checked_exceptions(ApiBuilder builder) {
		var api = builder.build("""
			class A {}
			class B extends Exception {}
			class C extends RuntimeException {}
			class D extends B {}
			class E extends C {}""");

		var a = assertClass(api, "A");
		assertFalse(a.isCheckedException());

		var b = assertClass(api, "B");
		assertTrue(b.isCheckedException());

		var c = assertClass(api, "C");
		assertFalse(c.isCheckedException());

		var d = assertClass(api, "D");
		assertTrue(d.isCheckedException());

		var e = assertClass(api, "E");
		assertFalse(e.isCheckedException());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void record_is_final(ApiBuilder builder) {
		var api = builder.build("public record A() {}");
		var a = assertRecord(api, "A");
		assertTrue(a.isFinal(), "Records should be implicitly final");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void abstract_sealed_class(ApiBuilder builder) {
		var api = builder.build("""
        public abstract sealed class A permits B {}
        final class B extends A {}
        """);
		var a = assertClass(api, "A");
		assertTrue(a.isAbstract());
		assertTrue(a.isSealed());

		var b = assertClass(api, "B");
		assertTrue(b.isFinal());
		assertFalse(b.isSealed());
		assertTrue(b.isFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void interface_is_implicitly_abstract(ApiBuilder builder) {
		var api = builder.build("public interface A {}");
		var a = assertInterface(api, "A");
		assertTrue(a.isAbstract());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void enum_is_final(ApiBuilder builder) {
		var api = builder.build("public enum A {}");
		var a = assertEnum(api, "A");
		assertTrue(a.isFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void enum_with_constant_specific_class_body(ApiBuilder builder) {
		var api = builder.build("""
        public enum A {
            ONE {
                @Override public String toString() { return "one"; }
            },
            TWO;
        }
        """);
		assertEnum(api, "A");
		// Even though ONE has its own class body, only the enum A should be extracted.
		assertThat(api.getAllTypes().toList(), hasSize(1));
	}
}
