package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.AccessModifier;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.assertNoField;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldsExtractionTest {
	@Test
	void fields_within_package_private_class() {
		var api = buildAPI("""
      class A {
        private int a;
        protected int b;
        public int c;
        int d;
      }""");

		var a = assertClass(api, "A", AccessModifier.PACKAGE_PRIVATE);
		assertFalse(a.isExported());
		assertThat(a.getFields(), is(empty()));
	}

	@Test
	void fields_within_public_class() {
		var api = buildAPI("""
      public class A {
        private int a;
        protected int b;
        public int c;
        int d;
      }""");

		var a = assertClass(api, "A", AccessModifier.PUBLIC);
		assertTrue(a.isExported());
		assertNoField(a, "a");
		assertField(a, "b", AccessModifier.PROTECTED);
		assertField(a, "c", AccessModifier.PUBLIC);
		assertNoField(a, "d");
	}
}
