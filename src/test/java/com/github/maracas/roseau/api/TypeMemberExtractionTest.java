package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.PrimitiveTypeReference;
import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertField;
import static com.github.maracas.roseau.TestUtils.assertInterface;
import static com.github.maracas.roseau.TestUtils.assertMethod;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
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

	@Test
	void void_members() {
		var api = buildAPI("""
			public interface A {
				void m();
			}""");

		var a = assertInterface(api, "A");
		var m = assertMethod(a, "m");

		assertThat(m.getType(), is(instanceOf(PrimitiveTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("void")));
	}

	@Test
	void primitive_members() {
		var api = buildAPI("""
			public interface A {
				int f = 2;
				int m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(PrimitiveTypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("int")));

		assertThat(m.getType(), is(instanceOf(PrimitiveTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("int")));
	}

	@Test
	void jdk_members() {
		var api = buildAPI("""
			public interface A {
				String f = 2;
				String m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(TypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("java.lang.String")));

		assertThat(m.getType(), is(instanceOf(TypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("java.lang.String")));
	}

	@Test
	void api_members() {
		var api = buildAPI("""
			public interface I {}
			public interface A {
				I f = null;
				I m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(TypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("I")));

		assertThat(m.getType(), is(instanceOf(TypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("I")));
	}

	@Test
	void unknown_members() {
		var api = buildAPI("""
			public interface A {
				U f = null;
				U m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(TypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("U")));

		assertThat(m.getType(), is(instanceOf(TypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("U")));
	}

	@Test
	void array_members() {
		var api = buildAPI("""
			public interface A {
				int[] f = {};
				int[] m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("int[]")));

		if (f.getType() instanceof ArrayTypeReference(var componentType))
			assertThat(componentType.getQualifiedName(), is(equalTo("int")));

		assertThat(m.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("int[]")));

		if (m.getType() instanceof ArrayTypeReference(var componentType))
			assertThat(componentType.getQualifiedName(), is(equalTo("int")));
	}

	@Test
	void multidimensional_array_members() {
		var api = buildAPI("""
			public interface A {
				int[][] f = {{}};
				int[][] m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("int[][]")));

		if (f.getType() instanceof ArrayTypeReference(var componentType)) {
			assertThat(componentType, is(instanceOf(ArrayTypeReference.class)));
			assertThat(componentType.getQualifiedName(), is(equalTo("int[]")));
			if (componentType instanceof ArrayTypeReference(var subcomponentType)) {
				assertThat(subcomponentType, is(instanceOf(PrimitiveTypeReference.class)));
				assertThat(subcomponentType.getQualifiedName(), is(equalTo("int")));
			}
		}

		assertThat(m.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("int[][]")));

		if (m.getType() instanceof ArrayTypeReference(var componentType)) {
			assertThat(componentType, is(instanceOf(ArrayTypeReference.class)));
			assertThat(componentType.getQualifiedName(), is(equalTo("int[]")));
			if (componentType instanceof ArrayTypeReference(var subcomponentType)) {
				assertThat(subcomponentType, is(instanceOf(PrimitiveTypeReference.class)));
				assertThat(subcomponentType.getQualifiedName(), is(equalTo("int")));
			}
		}
	}

	@Test
	void generic_members() {
		var api = buildAPI("""
			public class A<T> {
				public T f = null;
				public T m1() { return null; }
				public <U> U m2() { return null; }
			}""");

		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var m1 = assertMethod(a, "m1");
		var m2 = assertMethod(a, "m2");

		assertThat(f.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("T")));

		assertThat(m1.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(m1.getType().getQualifiedName(), is(equalTo("T")));

		assertThat(m2.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(m2.getType().getQualifiedName(), is(equalTo("U")));
	}

	@Test
	void generic_members_with_bounds() {
		var api = buildAPI("""
				public class A<T extends String> {
					public T f = null;
					public <U extends T> U m() { return null; }
				}""");

		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m");

		assertThat(f.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("T")));

		assertThat(m.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("U")));
	}
}
