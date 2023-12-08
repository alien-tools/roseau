package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
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

		var a = assertClass(api, "A");
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

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		var fb = assertField(a, "b");
		assertTrue(fb.isProtected());
		assertThat(fb.getContainingType().getResolvedApiType().get(), is(a));
		var fc = assertField(a, "c");
		assertTrue(fc.isPublic());
		assertThat(fc.getContainingType().getResolvedApiType().get(), is(a));
	}

	@Test
	void fields_within_nested_class() {
		var api = buildAPI("""
			public class B {
			  protected class A {
			    private int a;
			    protected int b;
			    public int c;
			    int d;
			  }
			 }""");

		var a = assertClass(api, "B$A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		var fb = assertField(a, "b");
		assertTrue(fb.isProtected());
		assertThat(fb.getContainingType().getResolvedApiType().get(), is(a));
		var fc = assertField(a, "c");
		assertTrue(fc.isPublic());
		assertThat(fc.getContainingType().getResolvedApiType().get(), is(a));
	}

	@Test
	void fields_within_final_class() {
		var api = buildAPI("""
			public final class A {
				private int a;
			  protected int b;
			  public int c;
			  int d;
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(1));
		var fc = assertField(a, "c");
		assertTrue(fc.isPublic());
		assertThat(fc.getContainingType().getResolvedApiType().get(), is(a));
	}

	@Test
	void fields_within_sealed_class() {
		var api = buildAPI("""
			public sealed class A {
				private int a;
			  protected int b;
			  public int c;
			  int d;
			}
			final class B extends A {}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(1));
		var fc = assertField(a, "c");
		assertTrue(fc.isPublic());
		assertThat(fc.getContainingType().getResolvedApiType().get(), is(a));
	}

	@Test
	void fields_within_effectively_final_class() {
		var api = buildAPI("""
			public class A {
				private int a;
			  protected int b;
			  public int c;
			  int d;
				A() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(1));
		var fc = assertField(a, "c");
		assertTrue(fc.isPublic());
		assertThat(fc.getContainingType().getResolvedApiType().get(), is(a));
	}

	@Test
	void final_fields() {
		var api = buildAPI("""
			public class A {
			  public int a;
			  public final int b;
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		var fa = assertField(a, "a");
		assertFalse(fa.isFinal());
		var fb = assertField(a, "b");
		assertTrue(fb.isFinal());
	}

	@Test
	void static_fields() {
		var api = buildAPI("""
			public class A {
			  public int a;
			  public static int b;
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		var fa = assertField(a, "a");
		assertFalse(fa.isStatic());
		var fb = assertField(a, "b");
		assertTrue(fb.isStatic());
	}
}
