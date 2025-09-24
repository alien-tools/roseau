package io.github.alien.roseau.diff;

import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.diff.changes.BreakingChangeKind.TYPE_REMOVED;
import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeRemovedTest {
	@Test
	void class_private_removed() {
		var v1 = "class A {}";
		var v2 = "class B {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_public_removed() {
		var v1 = "public class A {}";
		var v2 = "public class B {}";

		assertBC("A", TYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_public_kept() {
		var v1 = "public class A {}";
		var v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_public_moved() {
		var v1 = "public class A {}";
		var v2 = "public class B {}";

		assertBC("A", TYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_now_package_private() {
		var v1 = "public class A {}";
		var v2 = "class A {}";

		assertBC("A", TYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_inner_private_in_class_public_removed() {
		var v1 = """
			public class A {
			  class I {}
			}""";
		var v2 = """
			public class A {
			  class J {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_inner_protected_in_class_public_removed() {
		var v1 = """
			public class A {
			  protected class I {}
			}""";
		var v2 = """
			public class A {
			  protected class J {}
			}""";

		assertBC("A$I", TYPE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_public_in_class_public_removed() {
		var v1 = """
			public class A {
			  public class I {}
			}""";
		var v2 = """
			public class A {
			  public class J {}
			}""";

		assertBC("A$I", TYPE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_static_public_in_class_private_removed() {
		var v1 = """
			class A {
			  public static class I {}
			}""";
		var v2 = """
			class A {
			  public static class J {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_inner_static_protected_in_class_public_removed() {
		var v1 = """
			public class A {
			  static protected class I {}
			}""";
		var v2 = """
			public class A {
			  static protected class J {}
			}""";

		assertBC("A$I", TYPE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_static_public_in_class_public_removed() {
		var v1 = """
			public class A {
			  public static class I {}
			}""";
		var v2 = """
			public class A {
			  public static class J {}
			}""";

		assertBC("A$I", TYPE_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_public_static_in_class_private_removed() {
		var v1 = """
			class A {
			  public static class I {}
			}""";
		var v2 = """
			class A {
			  public static class J {}
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void annotation_interface_removed() {
		var v1 = "public @interface A {}";
		var v2 = "@interface A {}";

		assertBC("A", TYPE_REMOVED, 1, buildDiff(v1, v2));
	}
}
