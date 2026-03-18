package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityProviderTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void expression_compatibility_distinguishes_boxing_from_unboxing(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		assertThat(api.isExpressionCompatible(m, PrimitiveTypeReference.INT, new TypeReference<>("java.lang.Integer"))).isTrue();
		assertThat(api.isExpressionCompatible(m, new TypeReference<>("java.lang.Integer"), PrimitiveTypeReference.INT)).isFalse();
		assertThat(api.isExpressionCompatible(m, PrimitiveTypeReference.LONG, new TypeReference<>("java.lang.Integer"))).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void expression_compatibility_rejects_raw_mismatches(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		var rawList = new TypeReference<>("java.util.List");
		var rawArrayList = new TypeReference<>("java.util.ArrayList");
		var listOfString = new TypeReference<>("java.util.List", List.of(TypeReference.STRING));

		assertThat(api.isExpressionCompatible(m, rawList, rawArrayList)).isTrue();
		assertThat(api.isExpressionCompatible(m, rawList, listOfString)).isFalse();
		assertThat(api.isExpressionCompatible(m, listOfString, rawArrayList)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void return_type_substitutability_allows_unchecked_conversion_but_not_boxing(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		var rawList = new TypeReference<>("java.util.List");
		var rawArrayList = new TypeReference<>("java.util.ArrayList");
		var listOfString = new TypeReference<>("java.util.List", List.of(TypeReference.STRING));

		assertThat(api.isReturnTypeSubstitutable(m, rawList, listOfString)).isTrue();
		assertThat(api.isReturnTypeSubstitutable(m, rawArrayList, listOfString)).isTrue();
		assertThat(api.isReturnTypeSubstitutable(m, PrimitiveTypeReference.INT, new TypeReference<>("java.lang.Integer"))).isFalse();
	}
}
