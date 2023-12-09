package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.assertInterface;
import static com.github.maracas.roseau.TestUtils.assertMethod;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeMemberExtractionTest {
	@Test
	void members_within_package_private_class() {
		var api = buildAPI("""
			class A {
			  private int f1;
			  protected int f2;
			  public int f3;
			  int f4;
			  private void m1() {}
			  protected void m2() {}
			  public void m3() {}
			  void m4() {}
			}""");

		var a = assertClass(api, "A");
		assertFalse(a.isExported());
		assertThat(a.getFields(), is(empty()));
		assertThat(a.getMethods(), is(empty()));
	}

	@Test
	void members_within_public_class() {
		var api = buildAPI("""
			public class A {
			  private int f1;
			  protected int f2;
			  public int f3;
			  int f4;
			  private void m1() {}
			  protected void m2() {}
			  public void m3() {}
			  void m4() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		assertThat(a.getMethods(), hasSize(2));

		var f2 = assertField(a, "f2");
		var f3 = assertField(a, "f3");
		var m2 = assertMethod(a, "m2");
		var m3 = assertMethod(a, "m3");

		assertTrue(f2.isProtected());
		assertTrue(f3.isPublic());
		assertTrue(m2.isProtected());
		assertTrue(m3.isPublic());
	}

	@Test
	void members_within_public_interface() {
		var api = buildAPI("""
			public interface A {
			 	public int f1 = 0;
			 	int f2 = 0;
			 	public void m1();
			 	void m2();
			}""");

		var a = assertInterface(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		assertThat(a.getMethods(), hasSize(2));

		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

		assertTrue(f1.isPublic());
		assertTrue(f2.isPublic());
		assertTrue(m1.isPublic());
		assertTrue(m2.isPublic());
	}

	@Test
	void members_within_nested_class() {
		var api = buildAPI("""
			public class B {
			  protected class A {
			    private int f1;
			    protected int f2;
			    public int f3;
			    int f4;
			    private void m1() {}
			    protected void m2() {}
			    public void m3() {}
			    void m4() {}
			  }
			 }""");

		var a = assertClass(api, "B$A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		assertThat(a.getMethods(), hasSize(2));

		var f2 = assertField(a, "f2");
		var f3 = assertField(a, "f3");
		var m2 = assertMethod(a, "m2");
		var m3 = assertMethod(a, "m3");

		assertTrue(f2.isProtected());
		assertTrue(f3.isPublic());
		assertTrue(m2.isProtected());
		assertTrue(m3.isPublic());
	}

	@Test
	void members_within_final_class() {
		var api = buildAPI("""
			public final class A {
				private int f1;
			  protected int f2;
			  public int f3;
			  int f4;
			  private void m1() {}
			  protected void m2() {}
			  public void m3() {}
			  void m4() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(1));
		assertThat(a.getMethods(), hasSize(1));

		var f3 = assertField(a, "f3");
		var m3 = assertMethod(a, "m3");

		assertTrue(f3.isPublic());
		assertTrue(m3.isPublic());
	}

	@Test
	void members_within_sealed_class() {
		var api = buildAPI("""
			public sealed class A permits B {
				private int f1;
			  protected int f2;
			  public int f3;
			  int f4;
			  private void m1() {}
			  protected void m2() {}
			  public void m3() {}
			  void m4() {}
			}
			final class B extends A {}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(1));
		assertThat(a.getMethods(), hasSize(1));

		var f3 = assertField(a, "f3");
		var m3 = assertMethod(a, "m3");

		assertTrue(f3.isPublic());
		assertTrue(m3.isPublic());
	}

	@Test
	void members_within_effectively_final_class() {
		var api = buildAPI("""
			public class A {
			  A() {} // subclass-inaccessible constructor
				private int f1;
			  protected int f2;
			  public int f3;
			  int f4;
			  private void m1() {}
			  protected void m2() {}
			  public void m3() {}
			  void m4() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(1));
		assertThat(a.getMethods(), hasSize(1));

		var f3 = assertField(a, "f3");
		var m3 = assertMethod(a, "m3");

		assertTrue(f3.isPublic());
		assertTrue(m3.isPublic());
	}

	@Test
	void final_members() {
		var api = buildAPI("""
			public class A {
			  public int f1;
			  public final int f2 = 0;
			  public void m1() {}
			  public final void m2() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		assertThat(a.getMethods(), hasSize(2));

		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

		assertFalse(f1.isFinal());
		assertTrue(f2.isFinal());
		assertFalse(m1.isFinal());
		assertTrue(m2.isFinal());
	}

	@Test
	void static_members() {
		var api = buildAPI("""
			public class A {
			  public int f1;
			  public static int f2;
			  public void m1() {}
			  public static void m2() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getFields(), hasSize(2));
		assertThat(a.getMethods(), hasSize(2));

		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

		assertFalse(f1.isStatic());
		assertTrue(f2.isStatic());
		assertFalse(m1.isStatic());
		assertTrue(m2.isStatic());
	}
}
