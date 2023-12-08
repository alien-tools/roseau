package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedTypesExtractionTest {
	@Test
	void package_private_class_in_package_private_class() {
		var api = buildAPI("class A { class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPackagePrivate());
		assertFalse(b.isStatic());
	}

	@Test
	void package_private_static_class_in_package_private_class() {
		var api = buildAPI("class A { static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPackagePrivate());
		assertTrue(b.isStatic());
	}

	@Test
	void protected_class_in_package_private_class() {
		var api = buildAPI("class A { protected class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isProtected());
		assertFalse(b.isStatic());
	}

	@Test
	void protected_static_class_in_package_private_class() {
		var api = buildAPI("class A { protected static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isProtected());
		assertTrue(b.isStatic());
	}

	@Test
	void public_class_in_package_private_class() {
		var api = buildAPI("class A { public class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPublic());
		assertFalse(b.isStatic());
	}

	@Test
	void public_static_class_in_package_private_class() {
		var api = buildAPI("class A { public static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPublic());
		assertTrue(b.isStatic());
	}

	@Test
	void private_class_in_package_private_class() {
		var api = buildAPI("class A { private class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPrivate());
		assertFalse(b.isStatic());
	}

	@Test
	void private_static_class_in_package_private_class() {
		var api = buildAPI("class A { private static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPrivate());
		assertTrue(b.isStatic());
	}

	@Test
	void package_private_class_in_public_class() {
		var api = buildAPI("public class A { class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPackagePrivate());
		assertFalse(b.isStatic());
	}

	@Test
	void package_private_static_class_in_public_class() {
		var api = buildAPI("public class A { static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPackagePrivate());
		assertTrue(b.isStatic());
	}

	@Test
	void protected_class_in_public_class() {
		var api = buildAPI("public class A { protected class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isProtected());
		assertFalse(b.isStatic());
	}

	@Test
	void protected_static_class_in_public_class() {
		var api = buildAPI("public class A { protected static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isProtected());
		assertTrue(b.isStatic());
	}

	@Test
	void public_class_in_public_class() {
		var api = buildAPI("public class A { public class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isPublic());
		assertFalse(b.isStatic());
	}

	@Test
	void public_static_class_in_public_class() {
		var api = buildAPI("public class A { public static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isPublic());
		assertTrue(b.isStatic());
	}

	@Test
	void private_class_in_public_class() {
		var api = buildAPI("public class A { private class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPrivate());
		assertFalse(b.isStatic());
	}

	@Test
	void private_static_class_in_public_class() {
		var api = buildAPI("public class A { private static class B {} }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPrivate());
		assertTrue(b.isStatic());
	}

	@Test
	void doubly_nested_unexported_class() {
		var api = buildAPI("class A { public class B { class C {} } }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$B$C");

		assertFalse(a.isNested());
		assertFalse(a.isExported());
		assertTrue(a.isPackagePrivate());
		assertTrue(b.isNested());
		assertFalse(b.isExported());
		assertTrue(b.isPublic());
		assertFalse(b.isStatic());
		assertTrue(c.isNested());
		assertFalse(c.isExported());
		assertTrue(c.isPackagePrivate());
		assertFalse(c.isStatic());
	}

	@Test
	void doubly_nested_exported_class() {
		var api = buildAPI("public class A { protected class B { protected static class C {} } }");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var c = assertClass(api, "A$B$C");

		assertFalse(a.isNested());
		assertTrue(a.isExported());
		assertTrue(a.isPublic());
		assertTrue(b.isNested());
		assertTrue(b.isExported());
		assertTrue(b.isProtected());
		assertFalse(b.isStatic());
		assertTrue(c.isNested());
		assertTrue(c.isExported());
		assertTrue(c.isProtected());
		assertTrue(c.isStatic());
	}
}
