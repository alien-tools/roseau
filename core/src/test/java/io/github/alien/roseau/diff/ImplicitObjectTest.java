package io.github.alien.roseau.diff;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertBCs;
import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.bc;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;

class ImplicitObjectTest {
	@Client("Object a = new A();")
	@Test
	void class_now_object() {
		var v1 = "public class A {}";
		var v2 = "public class A extends Object {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("Object a = new A();")
	@Test
	void class_no_longer_object() {
		var v1 = "public class A extends Object {}";
		var v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a = new A() {};")
	@Test
	void object_methods_now_abstract() {
		var v1 = """
			public abstract class A {
				@Override public boolean equals(Object o) { return true; }
				public int hashCode() { return 0; }
				@Override public String toString() { return null; }
			}""";
		var v2 = """
			public abstract class A {
				@Override public abstract boolean equals(Object o);
				public abstract int hashCode();
				@Override public abstract String toString();
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "A.equals", BreakingChangeKind.METHOD_NOW_ABSTRACT, 2),
			bc("A", "A.hashCode", BreakingChangeKind.METHOD_NOW_ABSTRACT, 3),
			bc("A", "A.toString", BreakingChangeKind.METHOD_NOW_ABSTRACT, 4));
	}

	@Disabled("Technically breaking")
	@Client("A a = new A() {};")
	@Test
	void abstract_object_methods_added_to_abstract_class() {
		var v1 = "public abstract class A {}";
		var v2 = """
			public abstract class A {
				@Override public abstract boolean equals(Object o);
				public abstract int hashCode();
				@Override public abstract String toString();
			}""";

		assertBCs(buildDiff(v1, v2),
			bc("A", "java.lang.Object.equals", BreakingChangeKind.METHOD_NOW_ABSTRACT, -1),
			bc("A", "java.lang.Object.hashCode", BreakingChangeKind.METHOD_NOW_ABSTRACT, -1),
			bc("A", "java.lang.Object.toString", BreakingChangeKind.METHOD_NOW_ABSTRACT, -1));
	}

	@Client("A a = new A() {};")
	@Test
	void concrete_object_methods_added_to_abstract_class() {
		var v1 = "public abstract class A {}";
		var v2 = """
			public abstract class A {
				@Override public boolean equals(Object o) { return true; }
				public int hashCode() { return 0; }
				@Override public String toString() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("A a = new A();")
	@Test
	void concrete_object_methods_added_to_class() {
		var v1 = "public class A {}";
		var v2 = """
			public class A {
				@Override public boolean equals(Object o) { return true; }
				public int hashCode() { return 0; }
				@Override public String toString() { return null; }
			}""";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		A a = new A();
		int i = a.hashCode();
		String s = a.toString();
		boolean b = a.equals(a);""")
	@Test
	void object_methods_removed_from_class() {
		var v1 = """
			public class A {
				@Override public boolean equals(Object o) { return true; }
				public int hashCode() { return 0; }
				@Override public String toString() { return null; }
			}""";
		var v2 = "public class A {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Client("""
		I i = new I(){};
		int j = i.hashCode();
		String s = i.toString();
		boolean b = i.equals(i);""")
	@Test
	void object_methods_removed_from_interface() {
		var v1 = """
			public interface I {
				@Override boolean equals(Object o);
				int hashCode();
				@Override String toString();
			}""";
		var v2 = "public interface I {}";

		assertNoBC(buildDiff(v1, v2));
	}

	@Disabled("Technically breaking")
	@Client("I i = new I() {};")
	@Test
	void object_methods_added_to_interface() {
		var v1 = "public interface I {}";
		var v2 = """
			public interface I {
				@Override boolean equals(Object o);
				int hashCode();
				@Override String toString();
			}""";

		assertNoBC(buildDiff(v1, v2));
	}
}
