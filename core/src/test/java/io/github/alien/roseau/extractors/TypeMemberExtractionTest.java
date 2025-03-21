package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertField;
import static io.github.alien.roseau.utils.TestUtils.assertInterface;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeMemberExtractionTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_package_private_class(ApiBuilder builder) {
		var api = builder.build("""
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
		System.out.println(a.getDeclaredFields());
		assertThat(a.getDeclaredFields(), hasSize(2));
		assertThat(a.getDeclaredMethods(), hasSize(2));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_public_class(ApiBuilder builder) {
		var api = builder.build("""
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
		assertThat(a.getDeclaredFields(), hasSize(2));
		assertThat(a.getDeclaredMethods(), hasSize(2));

		var f2 = assertField(a, "f2");
		var f3 = assertField(a, "f3");
		var m2 = assertMethod(a, "m2()");
		var m3 = assertMethod(a, "m3()");

		assertTrue(f2.isProtected());
		assertTrue(f3.isPublic());
		assertTrue(m2.isProtected());
		assertTrue(m3.isPublic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_public_interface(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
			 	public int f1 = 0;
			 	int f2 = 0;
			 	public void m1();
			 	void m2();
			}""");

		var a = assertInterface(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getDeclaredFields(), hasSize(2));
		assertThat(a.getDeclaredMethods(), hasSize(2));

		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertTrue(f1.isPublic());
		assertTrue(f2.isPublic());
		assertTrue(m1.isPublic());
		assertTrue(m2.isPublic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_nested_class(ApiBuilder builder) {
		var api = builder.build("""
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
		assertThat(a.getDeclaredFields(), hasSize(2));
		assertThat(a.getDeclaredMethods(), hasSize(2));

		var f2 = assertField(a, "f2");
		var f3 = assertField(a, "f3");
		var m2 = assertMethod(a, "m2()");
		var m3 = assertMethod(a, "m3()");

		assertTrue(f2.isProtected());
		assertTrue(f3.isPublic());
		assertTrue(m2.isProtected());
		assertTrue(m3.isPublic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_final_class(ApiBuilder builder) {
		var api = builder.build("""
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
		assertThat(a.getDeclaredFields(), hasSize(1));
		assertThat(a.getDeclaredMethods(), hasSize(1));

		var f3 = assertField(a, "f3");
		var m3 = assertMethod(a, "m3()");

		assertTrue(f3.isPublic());
		assertTrue(m3.isPublic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_sealed_class(ApiBuilder builder) {
		var api = builder.build("""
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
		assertThat(a.getDeclaredFields(), hasSize(1));
		assertThat(a.getDeclaredMethods(), hasSize(1));

		var f3 = assertField(a, "f3");
		var m3 = assertMethod(a, "m3()");

		assertTrue(f3.isPublic());
		assertTrue(m3.isPublic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void members_within_effectively_final_class(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
			  private A() {} // subclass-inaccessible constructor
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
		assertThat(a.getDeclaredFields(), hasSize(1));
		assertThat(a.getDeclaredMethods(), hasSize(1));

		var f3 = assertField(a, "f3");
		var m3 = assertMethod(a, "m3()");

		assertTrue(f3.isPublic());
		assertTrue(m3.isPublic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void final_members(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
			  public int f1;
			  public final int f2 = 0;
			  public void m1() {}
			  public final void m2() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getDeclaredFields(), hasSize(2));
		assertThat(a.getDeclaredMethods(), hasSize(2));

		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertFalse(f1.isFinal());
		assertTrue(f2.isFinal());
		assertFalse(m1.isFinal());
		assertTrue(m2.isFinal());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void static_members(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
			  public int f1;
			  public static int f2;
			  public void m1() {}
			  public static void m2() {}
			}""");

		var a = assertClass(api, "A");
		assertTrue(a.isExported());
		assertThat(a.getDeclaredFields(), hasSize(2));
		assertThat(a.getDeclaredMethods(), hasSize(2));

		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertFalse(f1.isStatic());
		assertTrue(f2.isStatic());
		assertFalse(m1.isStatic());
		assertTrue(m2.isStatic());
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void void_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
				void m();
			}""");

		var a = assertInterface(api, "A");
		var m = assertMethod(a, "m()");

		assertThat(m.getType(), is(instanceOf(PrimitiveTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("void")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void primitive_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
				int f = 2;
				int m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m()");

		assertThat(f.getType(), is(instanceOf(PrimitiveTypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("int")));

		assertThat(m.getType(), is(instanceOf(PrimitiveTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("int")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void jdk_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
				String f = null;
				String m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m()");

		assertThat(f.getType(), is(instanceOf(TypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("java.lang.String")));

		assertThat(m.getType(), is(instanceOf(TypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("java.lang.String")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void api_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface I {}
			public interface A {
				I f = null;
				I m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m()");

		assertThat(f.getType(), is(instanceOf(TypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("I")));

		assertThat(m.getType(), is(instanceOf(TypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("I")));
	}

	@ParameterizedTest
	@EnumSource(value = ApiBuilderType.class, names = {"ASM", "JDT"}, mode = EnumSource.Mode.EXCLUDE)
	void unknown_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
				U f = null;
				U m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m()");

		assertThat(f.getType(), is(instanceOf(TypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("U")));

		assertThat(m.getType(), is(instanceOf(TypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("U")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void array_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
				int[] f = {};
				int[] m();
			}""");

		var a = assertInterface(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m()");

		assertThat(f.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("int[]")));

		if (f.getType() instanceof ArrayTypeReference(var componentType, var dimension)) {
			assertThat(componentType.getQualifiedName(), is(equalTo("int")));
			assertThat(dimension, is(equalTo(1)));
		}

		assertThat(m.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("int[]")));

		if (m.getType() instanceof ArrayTypeReference(var componentType, var dimension)) {
			assertThat(componentType.getQualifiedName(), is(equalTo("int")));
			assertThat(dimension, is(equalTo(1)));
		}
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void multidimensional_array_members(ApiBuilder builder) {
		var api = builder.build("""
			public interface A {
				String[][] f1 = {{""}};
				int[][] f2 = {{}};
				int[][] m();
			}""");

		var a = assertInterface(api, "A");
		var f1 = assertField(a, "f1");
		var f2 = assertField(a, "f2");
		var m = assertMethod(a, "m()");

		assertThat(f1.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(f1.getType().getQualifiedName(), is(equalTo("java.lang.String[][]")));
		assertThat(f2.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(f2.getType().getQualifiedName(), is(equalTo("int[][]")));

		if (f1.getType() instanceof ArrayTypeReference(var componentType, var dimension)) {
			assertThat(componentType, is(instanceOf(TypeReference.class)));
			assertThat(dimension, is(equalTo(2)));
			assertThat(componentType.getQualifiedName(), is(equalTo("java.lang.String")));
		}

		if (f2.getType() instanceof ArrayTypeReference(var componentType, var dimension)) {
			assertThat(componentType, is(instanceOf(PrimitiveTypeReference.class)));
			assertThat(dimension, is(equalTo(2)));
			assertThat(componentType.getQualifiedName(), is(equalTo("int")));
		}

		assertThat(m.getType(), is(instanceOf(ArrayTypeReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("int[][]")));

		if (m.getType() instanceof ArrayTypeReference(var componentType, var dimension)) {
			assertThat(componentType, is(instanceOf(PrimitiveTypeReference.class)));
			assertThat(dimension, is(equalTo(2)));
			assertThat(componentType.getQualifiedName(), is(equalTo("int")));
		}
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void generic_members(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T> {
				public T f = null;
				public T m1() { return null; }
				public <U> U m2() { return null; }
			}""");

		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var m1 = assertMethod(a, "m1()");
		var m2 = assertMethod(a, "m2()");

		assertThat(f.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("T")));

		assertThat(m1.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(m1.getType().getQualifiedName(), is(equalTo("T")));

		assertThat(m2.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(m2.getType().getQualifiedName(), is(equalTo("U")));
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void generic_members_with_bounds(ApiBuilder builder) {
		var api = builder.build("""
				public class A<T extends String> {
					public T f = null;
					public <U extends T> U m() { return null; }
				}""");

		var a = assertClass(api, "A");
		var f = assertField(a, "f");
		var m = assertMethod(a, "m()");

		assertThat(f.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(f.getType().getQualifiedName(), is(equalTo("T")));

		assertThat(m.getType(), is(instanceOf(TypeParameterReference.class)));
		assertThat(m.getType().getQualifiedName(), is(equalTo("U")));
	}
}
