package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBC;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;
import static org.assertj.core.api.Assertions.assertThat;

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

	@Client("A b = new B();")
	@Test
	void public_interface_removed() {
		var v1 = """
			public interface A {}
			public class B implements A {}""";
		var v2 = """
			public interface A {}
			public class B {}""";

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

	@Client("A c = new C();")
	@Test
	void public_interface_removed_indirect() {
		var v1 = """
			public interface A {}
			class B implements A {}
			public class C extends B {}""";
		var v2 = """
			public interface A {}
			class B {}
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

	@Client("java.util.ArrayList<String> a = new pkg.A();")
	@Test
	void external_superclass_removed() {
		var v1 = """
			module m { exports pkg; }
			package pkg;
			public class A extends java.util.ArrayList<String> {}""";
		var v2 = """
			module m { exports pkg; }
			package pkg;
			public class A {}""";

		var bcs = buildDiff(v1, v2).stream()
			.filter(bc -> bc.kind() == BreakingChangeKind.TYPE_SUPERTYPE_REMOVED)
			.toList();
		assertBC("pkg.A", "pkg.A", BreakingChangeKind.TYPE_SUPERTYPE_REMOVED, 3, bcs);
		assertThat(bcs).singleElement().extracting(BreakingChange::details)
			.isInstanceOfSatisfying(BreakingChangeDetails.TypeSupertypeRemoved.class,
				details -> assertThat(details.superType().getQualifiedName()).isEqualTo("java.util.ArrayList"));
	}
}
