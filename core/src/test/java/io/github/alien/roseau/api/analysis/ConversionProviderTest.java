package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import io.github.alien.roseau.utils.ApiBuilder;
import io.github.alien.roseau.utils.ApiBuilderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static io.github.alien.roseau.utils.TestUtils.assertClass;
import static io.github.alien.roseau.utils.TestUtils.assertMethod;
import static org.assertj.core.api.Assertions.assertThat;

class ConversionProviderTest {
	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void primitive_compatibility_distinguishes_assignment_of_values_from_expression_replacement(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		assertThat(api.isAssignmentCompatible(m, PrimitiveTypeReference.INT, PrimitiveTypeReference.LONG)).isTrue();
		assertThat(api.isAssignmentCompatible(m, PrimitiveTypeReference.INT, new TypeReference<>("java.lang.Integer"))).isFalse();
		assertThat(api.isAssignmentCompatible(m, PrimitiveTypeReference.INT, TypeReference.OBJECT)).isTrue();
		assertThat(api.isAssignmentCompatible(m, new TypeReference<>("java.lang.Integer"), PrimitiveTypeReference.INT)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(ApiBuilderType.class)
	void invocation_compatibility_tracks_parameter_variance(ApiBuilder builder) {
		var api = builder.build("""
			public class A {
				public void m() {}
			}""");
		var a = assertClass(api, "A");
		var m = assertMethod(api, a, "m()");

		var rawList = new TypeReference<>("java.util.List");
		var listOfString = new TypeReference<>("java.util.List", List.of(TypeReference.STRING));
		var listOfUnknown = new TypeReference<>("java.util.List", List.of(new WildcardTypeReference(List.of(TypeReference.OBJECT), true)));

		assertThat(api.isInvocationCompatible(m, listOfString, rawList)).isTrue();
		assertThat(api.isInvocationCompatible(m, listOfString, listOfUnknown)).isTrue();
		assertThat(api.isInvocationCompatible(m, rawList, listOfString)).isFalse();
		assertThat(api.isAssignmentCompatible(m, listOfString, rawList)).isTrue();
		assertThat(api.isAssignmentCompatible(m, rawList, listOfString)).isFalse();
	}
}
