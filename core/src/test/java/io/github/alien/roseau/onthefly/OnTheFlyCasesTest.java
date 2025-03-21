package io.github.alien.roseau.onthefly;

import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.OnTheFlyCaseCompiler.assertBC;
import static io.github.alien.roseau.utils.OnTheFlyCaseCompiler.assertNoBC;

class OnTheFlyCasesTest {
	// Source-breaking, binary-breaking
	@Test
	void class_now_abstract() {
		assertBC(
			"public class A {}",
			"public abstract class A {}",
			"new A();",
			"A", BreakingChangeKind.CLASS_NOW_ABSTRACT, 1);
	}

	// Source-breaking, binary-compatible
	@Test
	void constant_field_widening() {
		assertBC("""
			public interface A {
				public int f = 0;
			}""", """
			public interface A {
				public double f = 0;
			}""", """
				int i = A.f;
				System.out.println("i="+i);""",
			"A.f", BreakingChangeKind.FIELD_TYPE_CHANGED, 2);
	}

	// Source-compatible, binary-breaking
	@Test
	void method_param_type_boxing() {
		assertBC("""
			public class A {
				public void boxing(int i) {}
			}""", """
			public class A {
				public void boxing(Integer i) {}
			}""", """
				A a = new A();
				int i = 2;
				a.boxing(i);""",
			"A.boxing", BreakingChangeKind.METHOD_REMOVED, 2);
	}

	// Source-compatible, binary-compatible
	@Test
	void method_now_throws_unchecked() {
		assertNoBC("""
			public class A {
				public void m() {}
			}""", """
			public class A {
				public void m() throws RuntimeException {}
			}""", """
				A a = new A() {
					@Override public void m() {}
				};
				a.m();""");
	}
}
