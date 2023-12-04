package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.AccessModifier;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedTypesExtractionTest {
	@Test
	void package_private_class_in_package_private_class() {
		var api = buildAPI("class A { class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void package_private_static_class_in_package_private_class() {
		var api = buildAPI("class A { static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void protected_class_in_package_private_class() {
		var api = buildAPI("class A { protected class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PROTECTED);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void protected_static_class_in_package_private_class() {
		var api = buildAPI("class A { protected static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PROTECTED);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void public_class_in_package_private_class() {
		var api = buildAPI("class A { public class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PUBLIC);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void public_static_class_in_package_private_class() {
		var api = buildAPI("class A { public static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PUBLIC);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void private_class_in_package_private_class() {
		var api = buildAPI("class A { private class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PRIVATE);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void private_static_class_in_package_private_class() {
		var api = buildAPI("class A { private static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PRIVATE);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void package_private_class_in_public_class() {
		var api = buildAPI("public class A { class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void package_private_static_class_in_public_class() {
		var api = buildAPI("public class A { static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void protected_class_in_public_class() {
		var api = buildAPI("public class A { protected class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PROTECTED);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void protected_static_class_in_public_class() {
		var api = buildAPI("public class A { protected static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PROTECTED);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void public_class_in_public_class() {
		var api = buildAPI("public class A { public class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PUBLIC);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void public_static_class_in_public_class() {
		var api = buildAPI("public class A { public static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PUBLIC);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void private_class_in_public_class() {
		var api = buildAPI("public class A { private class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PRIVATE);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
	}

	@Test
	void private_static_class_in_public_class() {
		var api = buildAPI("public class A { private static class B {} }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PRIVATE);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isStatic());
	}

	@Test
	void doubly_nested_unexported_class() {
		var api = buildAPI("class A { public class B { class C {} } }");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		var b = assertClass(api, "A$B", AccessModifier.PUBLIC);
		var c = assertClass(api, "A$B$C", AccessModifier.PACKAGE_PRIVATE);

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertFalse(b.isStatic());
		assertTrue(c.isNested());
		assertFalse(c.isExported());
		assertFalse(c.isStatic());
	}

	@Test
	void doubly_nested_exported_class() {
		var api = buildAPI("public class A { protected class B { protected static class C {} } }");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		var b = assertClass(api, "A$B", AccessModifier.PROTECTED);
		var c = assertClass(api, "A$B$C", AccessModifier.PROTECTED);

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertFalse(b.isStatic());
		assertTrue(c.isNested());
		assertTrue(c.isExported());
		assertTrue(c.isStatic());
	}
}
