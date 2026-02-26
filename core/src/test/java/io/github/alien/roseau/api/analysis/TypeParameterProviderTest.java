package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;

class TypeParameterProviderTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void resolves_direct_and_recursive_bounds(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends Number, U extends T> {
				public <V extends U> void m(V v) {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m(java.lang.Number)");
		var t = new TypeParameterReference("T");
		var u = new TypeParameterReference("U");
		var v = new TypeParameterReference("V");
		var number = new TypeReference<>("java.lang.Number");

		assertThat(api.resolveDirectTypeParameterBound(m, t)).isEqualTo(number);
		assertThat(api.resolveDirectTypeParameterBound(m, u)).isEqualTo(t);
		assertThat(api.resolveDirectTypeParameterBound(m, v)).isEqualTo(u);

		assertThat(api.resolveTypeParameterBound(m, t)).isEqualTo(number);
		assertThat(api.resolveTypeParameterBound(m, u)).isEqualTo(number);
		assertThat(api.resolveTypeParameterBound(m, v)).isEqualTo(number);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void unresolved_type_parameter(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends Number, U extends T> {
				public <V extends U> void m(V v) {}
			}""");

		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m(java.lang.Number)");
		var x = new TypeParameterReference("X");

		assertThat(api.resolveTypeParameterBound(m, x)).isEqualTo(TypeReference.OBJECT);
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void nearest_scope_type_parameter_wins(ApiBuilder builder) {
		var api = builder.build("""
			public class A<T extends CharSequence> {
				public class B<T extends Number> {
					public T f;
					public <T> void m(T t) {}
				}
			}""");

		var a = assertClass(api, "A");
		var b = assertClass(api, "A$B");
		var m = assertMethod(api, b, "m(java.lang.Object)");
		var t = new TypeParameterReference("T");
		var number = new TypeReference<>("java.lang.Number");
		var charSequence = new TypeReference<>("java.lang.CharSequence");

		assertThat(api.resolveDirectTypeParameterBound(a, t)).isEqualTo(charSequence);
		assertThat(api.resolveDirectTypeParameterBound(b, t)).isEqualTo(number);
		assertThat(api.resolveDirectTypeParameterBound(m, t)).isEqualTo(TypeReference.OBJECT);
	}
}
