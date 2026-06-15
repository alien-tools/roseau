package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class TypeSupertypeRemovedTest {
	@Client("B b = new B(); // Can't upcast (A)")
	@Test
	void private_superclass_removed() {
		var v1 = """
			class A {}
			public class B extends A {}""";
		var v2 = """
			class A {}
			public class B {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		A b = new B();
		class X {
			A make() { return new B(); }
		}
		new X().make();""")
	@Test
	void public_superclass_removed() {
		var v1 = """
			public class A {}
			public class B extends A {}""";
		var v2 = """
			public class A {}
			public class B {}""";

		assertBC("B", "B", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("B c = new C(); // Can't upcast (A)")
	@Test
	void private_superclass_removed_indirect() {
		var v1 = """
			class A {}
			public class B extends A {}
			public class C extends B {}""";
		var v2 = """
			class A {}
			public class B {}
			public class C extends B {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		A c = new C();
		class X {
			A make() { return new C(); }
		}
		new X().make();""")
	@Test
	void public_superclass_removed_indirect() {
		var v1 = """
			public class A {}
			class B extends A {}
			public class C extends B {}""";
		var v2 = """
			public class A {}
			class B {}
			public class C extends B {}""";

		assertBC("C", "C", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("B b = new B(); // Can't upcast (A)")
	@Test
	void private_interface_removed() {
		var v1 = """
			interface A {}
			public class B implements A {}""";
		var v2 = """
			interface A {}
			public class B {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		A b = new B();
		b.m();""")
	@Test
	void public_interface_removed() {
		var v1 = """
			public interface A { void m(); }
			public class B implements A { public void m() {} }""";
		var v2 = """
			public interface A { void m(); }
			public class B { public void m() {} }""";

		assertBC("B", "B", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("B c = new C(); // Can't upcast (A)")
	@Test
	void private_interface_removed_indirect() {
		var v1 = """
			interface A {}
			public class B implements A {}
			public class C extends B {}""";
		var v2 = """
			interface A {}
			public class B {}
			public class C extends B {}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		A c = new C();
		c.m();""")
	@Test
	void public_interface_removed_indirect() {
		var v1 = """
			public interface A { void m(); }
			class B implements A { public void m() {} }
			public class C extends B {}""";
		var v2 = """
			public interface A { void m(); }
			class B { public void m() {} }
			public class C extends B {}""";

		assertBC("C", "C", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}

	@Client("A c = new C(){};")
	@Test
	void public_interface_extended_removed_indirect() {
		var v1 = """
			public interface A {}
			interface B extends A {}
			public interface C extends B {}""";
		var v2 = """
			public interface A {}
			interface B {}
			public interface C extends B {}""";

		assertBC("C", "C", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 1, buildDiff(v1, v2));
	}
}
