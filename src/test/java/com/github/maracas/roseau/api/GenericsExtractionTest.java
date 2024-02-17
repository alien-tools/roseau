package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;
import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.assertMethod;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class GenericsExtractionTest {
	@Test
	void single_type_parameter() {
		var api = buildAPI("class A<T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));
		assertThat(a.getFormalTypeParameters().getFirst().name(), is(equalTo("T")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds(), is(empty()));
	}

	@Test
	void type_parameter_with_class_bound() {
		var api = buildAPI("class A<T extends String> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));
		assertThat(a.getFormalTypeParameters().getFirst().name(), is(equalTo("T")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds(), hasSize(1));
		assertThat(a.getFormalTypeParameters().getFirst().bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
	}

	@Test
	void type_parameter_with_interface_bound() {
		var api = buildAPI("class A<T extends Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));
		assertThat(a.getFormalTypeParameters().getFirst().name(), is(equalTo("T")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds(), hasSize(1));
		assertThat(a.getFormalTypeParameters().getFirst().bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@Test
	void type_parameter_with_several_bounds() {
		var api = buildAPI("class A<T extends String & Runnable> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(1));
		assertThat(a.getFormalTypeParameters().getFirst().name(), is(equalTo("T")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds(), hasSize(2));
		assertThat(a.getFormalTypeParameters().getFirst().bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.String")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds().get(1).getQualifiedName(), is(equalTo("java.lang.Runnable")));
	}

	@Test
	void type_parameter_with_dependent_parameter_bound() {
		var api = buildAPI("class A<T, U extends T> {}");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(2));
		assertThat(a.getFormalTypeParameters().getFirst().name(), is(equalTo("T")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds(), is(empty()));
		assertThat(a.getFormalTypeParameters().get(1).name(), is(equalTo("U")));
		assertThat(a.getFormalTypeParameters().get(1).bounds(), hasSize(1));
		assertThat(a.getFormalTypeParameters().get(1).bounds().getFirst().getQualifiedName(), is(equalTo("T")));
	}

	@Test
	void type_parameter_with_dependent_class_bound() {
		var api = buildAPI("""
      class X {}
      class A<T, U extends X> {}""");

		var a = assertClass(api, "A");
		assertThat(a.getFormalTypeParameters(), hasSize(2));
		assertThat(a.getFormalTypeParameters().getFirst().name(), is(equalTo("T")));
		assertThat(a.getFormalTypeParameters().getFirst().bounds(), is(empty()));
		assertThat(a.getFormalTypeParameters().get(1).name(), is(equalTo("U")));
		assertThat(a.getFormalTypeParameters().get(1).bounds(), hasSize(1));
		assertThat(a.getFormalTypeParameters().get(1).bounds().getFirst().getQualifiedName(), is(equalTo("X")));
	}

	@Test
	void type_parameter_bounded_references() {
		var api = buildAPI("""
			public class C {
				public List<?> m1() { return null; }
				public List<? extends Number> m2() { return null; }
				public List<? super Number> m3() { return null; }
				public void m4(List<?> p) {}
				public void m5(List<? extends Number> p) {}
				public void m6(List<? super Number> p) {}
			}""");

		var c = assertClass(api, "C");
		var m1 = assertMethod(c, "m1");
		var m2 = assertMethod(c, "m2");
		var m3 = assertMethod(c, "m3");
		var m4 = assertMethod(c, "m4");
		var m5 = assertMethod(c, "m5");
		var m6 = assertMethod(c, "m6");

		assertThat(m1.getType(), instanceOf(TypeReference.class));
		if (m1.getType() instanceof TypeReference<?> typeRef) {
			assertThat(typeRef.getTypeArguments(), hasSize(1));
			assertThat(typeRef.getTypeArguments().getFirst(), instanceOf(WildcardTypeReference.class));
			if (typeRef.getTypeArguments().getFirst() instanceof WildcardTypeReference wcRef) {
				assertThat(wcRef.bounds(), hasSize(1));
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Object")));
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
				assertThat(wcRef.bounds().getFirst().getQualifiedName(), is(equalTo("java.lang.Object")));
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
}
