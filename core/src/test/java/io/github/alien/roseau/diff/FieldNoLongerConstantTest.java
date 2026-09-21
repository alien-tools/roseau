package io.github.alien.roseau.diff;

import io.github.alien.roseau.utils.Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.github.alien.roseau.utils.TestUtils.assertNoBC;
import static io.github.alien.roseau.utils.TestUtils.buildDiff;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code static final} field of primitive or {@code String} type initialized with a constant expression is a
 * <em>constant variable</em> (JLS §4.12.4). Clients may use it wherever the JLS requires a constant expression
 * (JLS §15.28): {@code case} labels, annotation element values, or the initializer of another constant variable.
 * <br>
 * Losing constant-variable status is source-breaking for such clients while remaining binary-compatible: clients
 * compiled against the old version inlined the value (JLS §13.1) and never reference the field at run time.
 */
class FieldNoLongerConstantTest {
	@Disabled("False negative: Roseau reports nothing, but the client below no longer compiles")
	@Client("""
		switch (args.length) {
			case A.X: break;
			default: break;
		}""")
	@Test
	void field_no_longer_final() {
		var v1 = """
			public class A {
				public static final int X = 1;
			}""";
		var v2 = """
			public class A {
				public static int X = 1;
			}""";

		// javac against v2: "constant expression required"
		assertThat(buildDiff(v1, v2)).isNotEmpty();
	}

	@Disabled("False negative: Roseau reports nothing, but the client below no longer compiles")
	@Client("""
		switch (args.length) {
			case A.X: break;
			default: break;
		}""")
	@Test
	void field_initializer_no_longer_constant_expression() {
		var v1 = """
			public class A {
				public static final int X = 1;
			}""";
		var v2 = """
			public class A {
				private static int compute() { return 1; }
				public static final int X = compute();
			}""";

		// The field keeps every modifier and its type; only the initializer stops being a constant expression, which
		// is enough to lose constant-variable status. javac against v2: "constant expression required"
		assertThat(buildDiff(v1, v2)).isNotEmpty();
	}

	@Client("java.util.List<String> l = A.X;")
	@Test
	void field_no_longer_final_was_never_constant() {
		var v1 = """
			public class A {
				public static final java.util.List<String> X = null;
			}""";
		var v2 = """
			public class A {
				public static java.util.List<String> X = null;
			}""";

		// Reference-typed fields are never constant variables, so reads keep compiling and linking
		assertNoBC(buildDiff(v1, v2));
	}
}
