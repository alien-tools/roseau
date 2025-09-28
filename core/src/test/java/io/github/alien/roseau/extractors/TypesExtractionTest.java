package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertAnnotation;
import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertEnum;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static io.github.alien.roseau.utils.TestUtils.assertNoConstructor;
import static io.github.alien.roseau.utils.TestUtils.assertRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypesExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_qualified_class(ApiBuilder builder) {
		var api = builder.build("""
			package pkg;
			public class A {}""");

		var a = assertClass(api, "pkg.A");

		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertThat(a.getQualifiedName()).isEqualTo("pkg.A");
		assertThat(a.getSimpleName()).isEqualTo("A");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_class(ApiBuilder builder) {
		var api = builder.build("class A {}");

		var a = assertClass(api, "A");

		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertThat(api.getExportedTypes()).isEmpty();
		assertThat(api.getLibraryTypes().getAllTypes()).hasSize(1);
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.OBJECT);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_class(ApiBuilder builder) {
		var api = builder.build("public class A {}");

		var a = assertClass(api, "A");

		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertThat(api.getExportedTypes()).hasSize(1);
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.OBJECT);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_interface(ApiBuilder builder) {
		var api = builder.build("interface A {}");

		var a = assertInterface(api, "A");

		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertThat(api.getExportedTypes()).isEmpty();
		assertThat(api.getLibraryTypes().getAllTypes()).hasSize(1);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_interface(ApiBuilder builder) {
		var api = builder.build("public interface A {}");

		var a = assertInterface(api, "A");

		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertThat(api.getExportedTypes()).hasSize(1);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_record(ApiBuilder builder) {
		var api = builder.build("record A() {}");

		var a = assertRecord(api, "A");

		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertThat(api.getExportedTypes()).isEmpty();
		assertThat(api.getLibraryTypes().getAllTypes()).hasSize(1);
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.RECORD);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_record(ApiBuilder builder) {
		var api = builder.build("public record A() {}");

		var a = assertRecord(api, "A");

		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertThat(api.getExportedTypes()).hasSize(1);
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.RECORD);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_enum(ApiBuilder builder) {
		var api = builder.build("enum A {}");

		var a = assertEnum(api, "A");

		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertThat(api.getExportedTypes()).isEmpty();
		assertThat(api.getLibraryTypes().getAllTypes()).hasSize(1);
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.ENUM);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_enum(ApiBuilder builder) {
		var api = builder.build("public enum A {}");

		var a = assertEnum(api, "A");

		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertThat(api.getExportedTypes()).hasSize(1);
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.ENUM);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void package_private_annotation(ApiBuilder builder) {
		var api = builder.build("@interface A {}");

		var a = assertAnnotation(api, "A");

		assertFalse(api.isExported(a));
		assertTrue(a.isPackagePrivate());
		assertThat(api.getLibraryTypes().getAllTypes()).hasSize(1);
		assertThat(api.getExportedTypes()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void public_annotation(ApiBuilder builder) {
		var api = builder.build("public @interface A {}");

		var a = assertAnnotation(api, "A");

		assertTrue(api.isExported(a));
		assertTrue(a.isPublic());
		assertThat(api.getExportedTypes()).hasSize(1);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void default_object_methods(ApiBuilder builder) {
		var api = builder.build("""
			public interface I {}
			public class C {}
			public record R() {}
			public @interface A {}
			public enum E {}""");

		var i = assertInterface(api, "I");
		var c = assertClass(api, "C");
		var r = assertRecord(api, "R");
		var a = assertAnnotation(api, "A");
		var e = assertEnum(api, "E");

		assertThat(i.getDeclaredMethods()).isEmpty();
		assertThat(api.getAllMethods(i)).hasSize(11); // java.lang.Object's methods
		assertThat(c.getDeclaredMethods()).isEmpty();
		assertThat(api.getAllMethods(c)).hasSize(11); // java.lang.Object's methods
		assertThat(r.getDeclaredMethods()).isEmpty();
		assertThat(api.getAllMethods(r)).hasSize(11); // java.lang.Object's methods
		assertThat(a.getDeclaredMethods()).isEmpty();
		assertThat(api.getAllMethods(a)).hasSize(11); // java.lang.Object's methods
		assertThat(e.getDeclaredMethods()).isEmpty();
		assertThat(api.getAllMethods(e)).hasSize(18); // java.lang.Enum's methods
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void default_constructors(ApiBuilder builder) {
		var api = builder.build("""
			public class C {}
			public record R() {}
			public enum E {}""");

		var c = assertClass(api, "C");
		var r = assertRecord(api, "R");
		var e = assertEnum(api, "E");

		assertConstructor(api, c, "<init>()");
		assertConstructor(api, r, "<init>()");
		assertNoConstructor(api, e, "<init>()");
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
		assertThat(api.getExportedTypes()).hasSize(1);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void anonymous_class_is_ignored(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public Runnable r1 = () -> {};
				public void m() {
					Runnable r2 = new Runnable() { public void run() {} };
				}
			}""");

		assertClass(api, "A");
		assertThat(api.getExportedTypes()).hasSize(1);
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
	void final_classes(ApiBuilder builder) {
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
			public class A {}
			public sealed class B permits C, D, E {}
			public sealed class C extends B permits F {}
			public final class D extends B {}
			public non-sealed class E extends B {}
			public final class F extends C {}""");

		var a = assertClass(api, "A");
		assertFalse(a.isFinal());
		assertFalse(a.isSealed());
		assertFalse(api.isEffectivelyFinal(a));

		var b = assertClass(api, "B");
		assertFalse(b.isFinal());
		assertTrue(b.isSealed());
		assertTrue(api.isEffectivelyFinal(b));
		assertThat(b.getPermittedTypes().stream().map(TypeReference::qualifiedName))
			.containsExactlyInAnyOrder("C", "D", "E");

		var c = assertClass(api, "C");
		assertFalse(c.isFinal());
		assertTrue(c.isSealed());
		assertTrue(api.isEffectivelyFinal(c));
		assertThat(c.getPermittedTypes().stream().map(TypeReference::qualifiedName))
			.containsExactlyInAnyOrder("F");

		var d = assertClass(api, "D");
		assertTrue(d.isFinal());
		assertFalse(d.isSealed());
		assertTrue(api.isEffectivelyFinal(d));

		var e = assertClass(api, "E");
		assertFalse(e.isFinal());
		assertFalse(e.isSealed());
		assertFalse(api.isEffectivelyFinal(e));
		// FIXME: No support for NON_SEALED in ASM yet
		if (builder != ApiBuilderType.ASM) {
			assertTrue(e.isNonSealed());
		}

		var f = assertClass(api, "F");
		assertTrue(f.isFinal());
		assertFalse(f.isSealed());
		assertTrue(api.isEffectivelyFinal(f));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void sealed_interfaces(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {}
			public sealed interface B permits C, D {}
			public sealed interface C extends B permits E {}
			public non-sealed interface D extends B {}
			public final class E implements C {}""");

		var a = assertInterface(api, "A");
		assertFalse(a.isFinal());
		assertFalse(a.isSealed());
		assertFalse(api.isEffectivelyFinal(a));

		var b = assertInterface(api, "B");
		assertFalse(b.isFinal());
		assertTrue(b.isSealed());
		assertTrue(api.isEffectivelyFinal(b));
		assertThat(b.getPermittedTypes().stream().map(TypeReference::qualifiedName))
			.containsExactlyInAnyOrder("C", "D");

		var c = assertInterface(api, "C");
		assertFalse(c.isFinal());
		assertTrue(c.isSealed());
		assertTrue(api.isEffectivelyFinal(c));
		assertThat(c.getPermittedTypes().stream().map(TypeReference::qualifiedName))
			.containsExactlyInAnyOrder("E");

		var d = assertInterface(api, "D");
		assertFalse(d.isFinal());
		assertFalse(d.isSealed());
		assertFalse(api.isEffectivelyFinal(d));

		var e = assertClass(api, "E");
		assertTrue(e.isFinal());
		assertFalse(e.isSealed());
		assertTrue(api.isEffectivelyFinal(e));
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"JDT"}, mode = EnumSource.Mode.EXCLUDE)
	void implicit_permits(ApiBuilder builder) {
		var api = builder.build("""
			public sealed interface I {
				record R1() implements I {}
				record R2() implements I {}
			}""");

		var i = assertInterface(api, "I");
		assertFalse(i.isFinal());
		assertTrue(i.isSealed());
		assertTrue(api.isEffectivelyFinal(i));
		assertThat(i.getPermittedTypes().stream().map(TypeReference::qualifiedName))
			.containsExactlyInAnyOrder("I$R1", "I$R2");
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void exception_types(ApiBuilder builder) {
		var api = builder.build("""
			class A {}
			class B extends Exception {}
			class C extends RuntimeException {}
			class D extends B {}
			class E extends C {}""");

		var a = assertClass(api, "A");
		assertFalse(api.isCheckedException(a));
		assertFalse(api.isUncheckedException(a));

		var b = assertClass(api, "B");
		assertTrue(api.isCheckedException(b));
		assertFalse(api.isUncheckedException(b));

		var c = assertClass(api, "C");
		assertFalse(api.isCheckedException(c));
		assertTrue(api.isUncheckedException(c));

		var d = assertClass(api, "D");
		assertTrue(api.isCheckedException(d));
		assertFalse(api.isUncheckedException(d));

		var e = assertClass(api, "E");
		assertFalse(api.isCheckedException(e));
		assertTrue(api.isUncheckedException(e));
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
	void record_inherits_record(ApiBuilder builder) {
		var api = builder.build("public record A() {}");
		var a = assertRecord(api, "A");
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.RECORD);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void record_no_synthetic_methods(ApiBuilder builder) {
		var api = builder.build("public record A() {}");
		var a = assertRecord(api, "A");
		assertThat(a.getDeclaredMethods()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void record_components(ApiBuilder builder) {
		var api = builder.build("public record A(int a, String b) {}");

		var a = assertRecord(api, "A");
		assertThat(a.getDeclaredMethods()).hasSize(2);
		assertConstructor(api, a, "<init>(int,java.lang.String)");

		var ma = assertMethod(api, a, "a()");
		var mb = assertMethod(api, a, "b()");
		assertThat(ma.getType()).isEqualTo(PrimitiveTypeReference.INT);
		assertThat(mb.getType()).isEqualTo(TypeReference.STRING);
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
		assertThat(a.getPermittedTypes().stream().map(TypeReference::qualifiedName))
			.containsExactlyInAnyOrder("B");

		var b = assertClass(api, "B");
		assertTrue(b.isFinal());
		assertFalse(b.isSealed());
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
		var a = assertEnum(api, "A");
		// Even though ONE has its own class body, only the enum A should be extracted.
		assertThat(api.getExportedTypes()).hasSize(1);
		// §8.9: An enum class E is implicitly sealed if its declaration contains
		// at least one enum constant that has a class bod
		assertThat(a.isSealed()).isTrue();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void enum_inherits_enum(ApiBuilder builder) {
		var api = builder.build("public enum A { X; }");
		var a = assertEnum(api, "A");
		assertThat(a.getSuperClass()).isEqualTo(TypeReference.ENUM);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void enum_no_synthetic_methods(ApiBuilder builder) {
		var api = builder.build("public enum A { X; }");
		var a = assertEnum(api, "A");
		assertThat(a.getDeclaredMethods()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void type_names(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T> {
				public class B<U> {
					public class C extends B {}
				}
			}""");

		assertClass(api, "A");
		assertClass(api, "A$B");
		assertClass(api, "A$B$C");
	}
}
