package com.github.maracas.roseau.api;

import org.junit.jupiter.api.Test;

import static com.github.maracas.roseau.TestUtils.assertClass;
import static com.github.maracas.roseau.TestUtils.buildAPI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
}
