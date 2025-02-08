package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.reference.TypeParameterReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.maracas.roseau.utils.TestUtils.assertClass;
import static com.github.maracas.roseau.utils.TestUtils.assertMethod;
import static com.github.maracas.roseau.utils.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class GenericsExtractionTest {
	@Test
	void single_type_parameter() {
		var api = buildAPI("class A<T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
	}

	@Test
	void type_parameter_with_class_bound() {
		var api = buildAPI("class A<T extends String> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(1));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
	}

	@Test
	void type_parameter_with_interface_bound() {
		var api = buildAPI("class A<T extends Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(1));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@Test
	void type_parameter_with_several_bounds() {
		var api = buildAPI("class A<T extends String & Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));

		var t = a.getFormalTypeParameters().getFirst();
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), hasSize(2));
		assertThat(t.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
		assertThat(t.bounds().get(1).getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@Test
	void type_parameter_with_dependent_parameter_bound() {
		var api = buildAPI("class A<T, U extends T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(2));

		var t = a.getFormalTypeParameters().getFirst();
		var u = a.getFormalTypeParameters().get(1);
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
		assertThat(u.name(), is(equalTo("U")));
		assertThat(u.bounds(), hasSize(1));
		assertThat(u.bounds().getFirst().getQualifiedName(), is(equalTo("T")));
	}

	@Test
	void type_parameter_with_dependent_class_bound() {
		var api = buildAPI("""
      class X {}
      class A<T, U extends X> {}""");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(2));

		var t = a.getFormalTypeParameters().getFirst();
		var u = a.getFormalTypeParameters().get(1);
		assertThat(t.name(), is(equalTo("T")));
		assertThat(t.bounds(), is(equalTo(List.of(TypeReference.OBJECT))));
		assertThat(u.name(), is(equalTo("U")));
		assertThat(u.bounds(), hasSize(1));
		assertThat(u.bounds().getFirst().getQualifiedName(), is(equalTo("X")));
	}

	@Test
	void type_parameter_bounded_references() {
		var api = buildAPI("""
			public class C {
				public List<?> m1() { return null; }
				public List<? extends Number> m2() { return null; }
				public List<? super Number> m3() { return null; }
				public void m4(java.util.List<?> p) {}
				public void m5(java.util.List<? extends Number> p) {}
				public void m6(java.util.List<? super Number> p) {}
			}""");

		var c = assertClass(api, "C");
		var m1 = assertMethod(c, "m1()");
		var m2 = assertMethod(c, "m2()");
		var m3 = assertMethod(c, "m3()");
		var m4 = assertMethod(c, "m4(java.util.List)");
		var m5 = assertMethod(c, "m5(java.util.List)");
		var m6 = assertMethod(c, "m6(java.util.List)");

		assertThat(m1.getType(), instanceOf(TypeReference.class));
		if (m1.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m2.getType(), instanceOf(TypeReference.class));
		if (m2.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m3.getType(), instanceOf(TypeReference.class));
		if (m3.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(false));
			}
		}

		assertThat(m4.getParameters().getFirst().type(), instanceOf(TypeReference.class));
		if (m4.getParameters().getFirst().type() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst(), is(equalTo(TypeReference.OBJECT)));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m5.getParameters().getFirst().type(), instanceOf(TypeReference.class));
		if (m5.getParameters().getFirst().type() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(true));
			}
		}

		assertThat(m6.getParameters().getFirst().type(), instanceOf(TypeReference.class));
		if (m6.getParameters().getFirst().type() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Number")));
				assertThat(wcRef.upper(), is(false));
			}
		}
	}

	@Test
	void throwable_generic() {
		var api = buildAPI("""
			public class A {
				public <X extends Throwable> void m() throws X {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(a, "m()");

		assertThat(m.getThrownExceptions().getFirst(), is(instanceOf(TypeParameterReference.class)));
	}

	@Test
	void method_type_parameter_resolution() {
		var api = buildAPI("""
			public class A<T extends String> {
				public class B<U extends Number> {
					public <V> void m(T t, U v, V u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(b, "m(java.lang.String,java.lang.Number,java.lang.Object)");
	}

	@Test
	void method_type_parameter_resolution_hiding() {
		var api = buildAPI("""
			public class A<T extends String> {
				public class B<T extends Number> {
					public <T> void m(T t, T v, T u) {}
				}
			}""");

		var b = assertClass(api, "A$B");
		assertMethod(b, "m(java.lang.Object,java.lang.Object,java.lang.Object)");
	}

	@Test
	void class_type_parameter_resolution() {
		var api = buildAPI("""
			public class A<T, U extends String, V extends Number> extends ArrayList<U> implements Supplier<V> {
				public V get() { return null; }
			}""");

		var a = assertClass(api, "A");
		var u = a.getFormalTypeParameters().get(1);
		var v = a.getFormalTypeParameters().get(2);
		var uRef = a.getSuperClass().get().getTypeArguments().getFirst();
		var vRef = a.getImplementedInterfaces().getFirst().getTypeArguments().getFirst();
		var get = assertMethod(a, "get()");
		var mvRef = get.getType();

		if (uRef instanceof TypeParameterReference tpr)
			assertThat(a.resolveTypeParameter(tpr).get(), is(equalTo(u)));
		else fail();

		if (vRef instanceof TypeParameterReference tpr)
			assertThat(a.resolveTypeParameter(tpr).get(), is(equalTo(v)));
		else fail();

		if (mvRef instanceof TypeParameterReference tpr)
			assertThat(get.resolveTypeParameter(tpr).get(), is(equalTo(v)));
		else fail();
	}
}
