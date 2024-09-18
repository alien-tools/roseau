package com.github.maracas.roseau.usage;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.usage.UseType.FIELD_READ;
import static com.github.maracas.roseau.usage.UseType.FIELD_WRITE;
import static com.github.maracas.roseau.utils.TestUtils.assertUsage;
import static com.github.maracas.roseau.utils.TestUtils.buildUsage;

class UsageTest {
	@Test
	void field_read() {
		var usage = buildUsage("""
			public class C {
				public int f;
			}""", """
			class A {
				void m() {
					int i = new C().f;
				}
			}""");

		System.out.println(usage);
		assertUsage("C.f", FIELD_READ, 3, usage);
	}

	@Test
	void field_write() {
		var usage = buildUsage("""
			public class C {
				public int f;
			}""", """
			class A {
				void m() {
					new C().f = 2;
				}
			}""");

		assertUsage("C.f", FIELD_WRITE, 3, usage);
	}
}
