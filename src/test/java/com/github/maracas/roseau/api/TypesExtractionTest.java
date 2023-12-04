package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.AccessModifier;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertAnnotation;
import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertEnum;
import static com.github.maracas.roseau.TestUtils.assertInterface;
import static com.github.maracas.roseau.TestUtils.assertRecord;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypesExtractionTest {
	@Test
	void package_private_class() {
		var api = buildAPI("class A {}");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void public_class() {
		var api = buildAPI("public class A {}");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);

		assertTrue(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void package_private_interface() {
		var api = buildAPI("interface A {}");

		var a = assertInterface(api, "A", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void public_interface() {
		var api = buildAPI("public interface A {}");

		var a = assertInterface(api, "A", AccessModifier.PUBLIC);

		assertTrue(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void package_private_record() {
		var api = buildAPI("record A {}");

		var a = assertRecord(api, "A", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void public_record() {
		var api = buildAPI("public record A {}");

		var a = assertRecord(api, "A", AccessModifier.PUBLIC);

		assertTrue(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void package_private_enum() {
		var api = buildAPI("enum A {}");

		var a = assertEnum(api, "A", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void public_enum() {
		var api = buildAPI("public enum A {}");

		var a = assertEnum(api, "A", AccessModifier.PUBLIC);

		assertTrue(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void package_private_annotation() {
		var api = buildAPI("@interface A {}");

		var a = assertAnnotation(api, "A", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void public_annotation() {
		var api = buildAPI("public @interface A {}");

		var a = assertAnnotation(api, "A", AccessModifier.PUBLIC);

		assertTrue(a.isExported());
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void local_class_is_ignored() {
		var api = buildAPI("""
			public class A {
				public void m() {
					class Local {}
				}
			}""");

		assertClass(api, "A", AccessModifier.PUBLIC);
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void anonymous_class_is_ignored() {
		var api = buildAPI("""
			public class A {
				public void m() {
					Runnable r = () -> {};
				}
			}""");

		assertClass(api, "A", AccessModifier.PUBLIC);
		assertThat(api.getAllTypes(), hasSize(1));
	}

	@Test
	void abstract_classes() {
		var api = buildAPI("""
        public class A {}
        public abstract class B {}""");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		assertFalse(a.isAbstract());

		var b = assertClass(api, "B", AccessModifier.PUBLIC);
		assertTrue(b.isAbstract());
	}

	@Test
	void final_classes() {
		var api = buildAPI("""
        public class A {}
        public final class B {}""");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		assertFalse(a.isFinal());

		var b = assertClass(api, "B", AccessModifier.PUBLIC);
		assertTrue(b.isFinal());
	}

	@Test
	void sealed_classes() {
		var api = buildAPI("""
			class A {}
			sealed class B permits C, D, E {}
			sealed class C extends B permits F {}
			final class D extends B {}
			non-sealed class E extends B {}
			final class F extends C {}""");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(a.isFinal());
		assertFalse(a.isSealed());
		assertFalse(a.isEffectivelyFinal());

		var b = assertClass(api, "B", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(b.isFinal());
		assertTrue(b.isSealed());
		assertTrue(b.isEffectivelyFinal());

		var c = assertClass(api, "C", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(c.isFinal());
		assertTrue(c.isSealed());
		assertTrue(c.isEffectivelyFinal());

		var d = assertClass(api, "D", AccessModifier.PACKAGE_PRIVATE);
		assertTrue(d.isFinal());
		assertFalse(d.isSealed());
		assertTrue(d.isEffectivelyFinal());

		var e = assertClass(api, "E", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(e.isFinal());
		assertFalse(e.isSealed());
		assertFalse(e.isEffectivelyFinal());

		var f = assertClass(api, "F", AccessModifier.PACKAGE_PRIVATE);
		assertTrue(f.isFinal());
		assertFalse(f.isSealed());
		assertTrue(f.isEffectivelyFinal());
	}

	@Test
	void checked_exceptions() {
		var api = buildAPI("""
			class A {}
			class B extends Exception {}
			class C extends RuntimeException {}
			class D extends B {}
			class E extends C {}""");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(a.isCheckedException());

		var b = assertClass(api, "B", AccessModifier.PACKAGE_PRIVATE);
		assertTrue(b.isCheckedException());

		var c = assertClass(api, "C", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(c.isCheckedException());

		var d = assertClass(api, "D", AccessModifier.PACKAGE_PRIVATE);
		assertTrue(d.isCheckedException());

		var e = assertClass(api, "E", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(e.isCheckedException());
	}
}
