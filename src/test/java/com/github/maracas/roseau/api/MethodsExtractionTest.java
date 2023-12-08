package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.assertMethod;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsExtractionTest {
	@Test
	void methods_within_package_private_class() {
		var api = buildAPI("""
      class A {
        private void m1() {}
        protected void m2() {}
        public void m3() {}
        void m4();
      }""");

		var a = assertClass(api, "A");
		assertFalse(a.isExported());
		assertThat(a.getMethods(), is(empty()));
	}

	@Test
	void methods_within_public_class() {
		var api = buildAPI("""
      public class A {
        private void m1() {}
        protected void m2() {}
        public void m3() {}
        void m4();
      }""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getMethods(), hasSize(2));
		var m2 = assertMethod(a, "m2");
		assertTrue(m2.isProtected());
		assertThat(m2.getContainingType().getResolvedApiType().get(), is(a));
		var m3 = assertMethod(a, "m3");
		assertTrue(m3.isPublic());
		assertThat(m3.getContainingType().getResolvedApiType().get(), is(a));
	}

	@Test
	void methods_within_nested_class() {
		var api = buildAPI("""
      public class B {
        protected class A {
          private void m1() {}
          protected void m2() {}
          public void m3() {}
          void m4();
        }
       }""");

		var a = assertClass(api, "B$A");
		assertTrue(a.isExported());
		assertThat(a.getMethods(), hasSize(2));
		var m2 = assertMethod(a, "m2");
		assertTrue(m2.isProtected());
		assertThat(m2.getContainingType().getResolvedApiType().get(), is(a));
		var m3 = assertMethod(a, "m3");
		assertTrue(m3.isPublic());
		assertThat(m3.getContainingType().getResolvedApiType().get(), is(a));
	}
}
