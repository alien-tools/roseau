package io.github.alien.roseau.extractors;

import com.cedarsoftware.util.DeepEquals;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class EqualityTest {
	@Test
	void extractors_produce_equal_apis() {
		var api = """
			package p1;
			@Deprecated
			@SuppressWarnings("unchecked")
			public class C {
				public int f;
				public E m() { return E.A; }
			}
			public enum E { A, B; }""";

		var spoon = TestUtils.buildSpoonAPI(api);
		var jdt = TestUtils.buildJdtAPI(api);
		var asm = TestUtils.buildAsmAPI(api);

		System.out.println(spoon);
		System.out.println(jdt);
		System.out.println(asm);

		var opts = new HashMap<String, Object>();
		DeepEquals.deepEquals(spoon, jdt, opts);
		System.out.println(opts);

		assertThat(spoon).isEqualTo(jdt);
	}
}
