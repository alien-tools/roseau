package io.github.alien.roseau.extractors;

import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertAnnotation;
import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertEnum;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedTypesExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPackagePrivate());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_static_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPackagePrivate());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void protected_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { protected class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isProtected());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void protected_static_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { protected static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isProtected());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { public class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPublic());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_static_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { public static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPublic());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void private_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { private class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPrivate());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void private_static_class_in_package_private_class(ApiBuilder builder) {
		var api = builder.build("class A { private static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPrivate());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPackagePrivate());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_static_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPackagePrivate());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void protected_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { protected class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isProtected());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void protected_static_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { protected static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isProtected());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { public class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isPublic());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_static_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { public static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isPublic());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void private_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { private class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPrivate());
		assertFalse(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void private_static_class_in_public_class(ApiBuilder builder) {
		var api = builder.build("public class A { private static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPrivate());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void doubly_nested_unexported_class(ApiBuilder builder) {
		var api = builder.build("class A { public class B { class C {} } }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$B$C");

		assertFalse(a.isNested());
		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(api.isExported(b));
		assertTrue(b.isPublic());
		assertFalse(b.isStatic());
		assertTrue(c.isNested());
		assertFalse(api.isExported(c));
		assertTrue(c.isPackagePrivate());
		assertFalse(c.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void doubly_nested_exported_class(ApiBuilder builder) {
		var api = builder.build("public class A { protected class B { protected static class C {} } }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$B$C");

		assertFalse(a.isNested());
		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isProtected());
		assertFalse(b.isStatic());
		assertTrue(c.isNested());
		assertTrue(api.isExported(c));
		assertTrue(c.isProtected());
		assertTrue(c.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_class_in_interface(ApiBuilder builder) {
		var api = builder.build("public interface A { class B {} }");
		assertInterface(api, "A");
		var b = assertClass(api, "A$B");
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isPublic());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_interface_in_class(ApiBuilder builder) {
		var api = builder.build("public class A { interface B {} }");
		assertClass(api, "A");
		var b = assertInterface(api, "A$B");
		assertTrue(b.isNested());
		// Without an explicit modifier, the nested interface remains package-private.
		assertFalse(api.isExported(b));
		// Member interfaces are implicitly static.
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_public_interface_in_class(ApiBuilder builder) {
		var api = builder.build("public class A { public interface B {} }");
		assertClass(api, "A");
		var b = assertInterface(api, "A$B");
		assertTrue(b.isNested());
		assertTrue(api.isExported(b));
		assertTrue(b.isPublic());
		assertTrue(b.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_enum_in_class(ApiBuilder builder) {
		var api = builder.build("public class A { enum B {} }");
		assertClass(api, "A");
		var b = assertEnum(api, "A$B");
		assertTrue(b.isNested());
		// Enums declared in a class are implicitly static.
		assertTrue(b.isStatic());
		// Without an explicit modifier, the nested enum is package-private.
		assertFalse(api.isExported(b));
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class)
	void nested_record_in_class(ApiBuilder builder) {
		var api = builder.build("public class A { record B() {} }");
		assertClass(api, "A");
		var b = assertRecord(api, "A$B");
		assertTrue(b.isNested());
		// Member records are implicitly static.
		assertTrue(b.isStatic());
		assertFalse(api.isExported(b));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_annotation_in_class(ApiBuilder builder) {
		var api = builder.build("public class A { @interface B {} }");
		assertClass(api, "A");
		var b = assertAnnotation(api, "A$B");
		assertTrue(b.isNested());
		// Nested annotation types are implicitly static.
		assertTrue(b.isStatic());
		// Without an explicit modifier, they are package-private.
		assertFalse(api.isExported(b));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_types_in_enum(ApiBuilder builder) {
		var api = builder.build("""
        public enum A {
          ONE, TWO;
          class B {}
          interface C {}
          enum D {}
          @interface E {}
        }
        """);
		assertEnum(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertInterface(api, "A$C");
		var d = assertEnum(api, "A$D");
		var e = assertAnnotation(api, "A$E");
		assertTrue(b.isNested());
		assertFalse(b.isStatic());
		assertFalse(api.isExported(b));
		assertTrue(c.isNested());
		assertTrue(c.isStatic());
		assertFalse(api.isExported(c));
		assertTrue(d.isNested());
		assertTrue(d.isStatic());
		assertFalse(api.isExported(d));
		assertTrue(e.isNested());
		assertTrue(e.isStatic());
		assertFalse(api.isExported(e));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_interface_in_record(ApiBuilder builder) {
		var api = builder.build("public record A() { interface B {} }");
		assertRecord(api, "A");
		var b = assertInterface(api, "A$B");
		assertTrue(b.isNested());
		// Nested interfaces in records are implicitly static.
		assertTrue(b.isStatic());
		assertFalse(api.isExported(b));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_annotation_in_annotation(ApiBuilder builder) {
		var api = builder.build("@interface A { @interface B {} }");
		assertAnnotation(api, "A");
		var b = assertAnnotation(api, "A$B");
		assertTrue(b.isNested());
		assertTrue(b.isStatic());
		assertFalse(api.isExported(b));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_sealed_class(ApiBuilder builder) {
		var api = builder.build("""
        public class A {
          sealed class B permits C {}
          final class C extends B {}
        }
        """);
		assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$C");
		assertTrue(b.isNested());
		// Member classes without an explicit static modifier remain inner classes.
		assertFalse(b.isStatic());
		assertTrue(b.isSealed());
		assertTrue(c.isNested());
		assertFalse(c.isStatic());
		assertTrue(c.isFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void triply_nested_class(ApiBuilder builder) {
		var api = builder.build("public class A { public class B { private static class C {} } }");
		assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$B$C");
		// B is a non-static member class.
		assertTrue(b.isNested());
		assertFalse(b.isStatic());
		// C is a static member of B.
		assertTrue(c.isNested());
		assertTrue(c.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nested_constructors(ApiBuilder builder) {
		var api = builder.build("""
        public class A {
          public class B {}
          public static class C {}
        }""");

		assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$C");

		assertConstructor(api, b, "<init>()");
		assertConstructor(api, c, "<init>()");
	}
}
