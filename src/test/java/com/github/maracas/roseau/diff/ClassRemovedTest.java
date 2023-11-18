package com.github.maracas.roseau.diff;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.diff.DiffTestUtils.assertBC;
import static com.github.maracas.roseau.diff.DiffTestUtils.assertNoBC;
import static com.github.maracas.roseau.diff.DiffTestUtils.buildDiff;
import static com.github.maracas.roseau.diff.changes.BreakingChangeKind.CLASS_REMOVED;

class ClassRemovedTest {
	@Test
	void class_private_removed() {
		String v1 = "class A {}";
		String v2 = "class B {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_public_removed() {
		String v1 = "public class A {}";
		String v2 = "public class B {}";

		assertBC("A", CLASS_REMOVED, 1, buildDiff(v1, v2));
	}

	@Test
	void class_public_kept() {
		String v1 = "public class A {}";
		String v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_public_moved() {
		String v1 = """
      package a;
      public class A {}""";
		String v2 = """
      package b;
      public class A {}""";

		assertBC("a.A", CLASS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_private_in_class_public_removed() {
		String v1 = """
      public class A {
        class I {}
      }""";
		String v2 = """
      public class A {
        class J {}
      }""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_inner_protected_in_class_public_removed() {
		String v1 = """
      public class A {
        protected class I {}
      }""";
		String v2 = """
      public class A {
        protected class J {}
      }""";

		assertBC("A$I", CLASS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_public_in_class_public_removed() {
		String v1 = """
      public class A {
        public class I {}
      }""";
		String v2 = """
      public class A {
        public class J {}
      }""";

		assertBC("A$I", CLASS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_static_public_in_class_private_removed() {
		String v1 = """
      class A {
        public static class I {}
      }""";
		String v2 = """
      class A {
        public static class J {}
      }""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Test
	void class_inner_static_protected_in_class_public_removed() {
		String v1 = """
      public class A {
        static protected class I {}
      }""";
		String v2 = """
      public class A {
        static protected class J {}
      }""";

		assertBC("A$I", CLASS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_static_public_in_class_public_removed() {
		String v1 = """
      public class A {
        public static class I {}
      }""";
		String v2 = """
      public class A {
        public static class J {}
      }""";

		assertBC("A$I", CLASS_REMOVED, 2, buildDiff(v1, v2));
	}

	@Test
	void class_inner_public_static_in_class_private_removed() {
		String v1 = """
      class A {
        public static class I {}
      }""";
		String v2 = """
      class A {
        public static class J {}
      }""";

		assertNoBC(buildDiff(v1, v2));
	}
}
