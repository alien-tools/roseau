package io.github.alien.roseau.api.model.reference;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TypeReferencesTest {
	@Test
	void raw_and_parameterized_references_are_distinct() {
		var raw = new TypeReference<>("java.util.List");
		var parameterized = new TypeReference<>("java.util.List",
			List.of(TypeReference.STRING));

		assertThat(raw.getQualifiedName()).isEqualTo(parameterized.getQualifiedName());
		assertThat(raw).isNotEqualTo(parameterized);
		assertThat(raw).hasToString("java.util.List");
		assertThat(parameterized).hasToString("java.util.List<java.lang.String>");
	}

	@Test
	void nested_type_arguments_are_rendered_recursively() {
		var ref = new TypeReference<>("java.util.Map",
			List.of(TypeReference.STRING, new TypeReference<>("java.util.List", List.of(new TypeParameterReference("T")))));

		assertThat(ref).hasToString("java.util.Map<java.lang.String,java.util.List<T>>");
	}

	@Test
	void array_references_expose_their_dimension_in_their_qualified_name() {
		var oneDim = new ArrayTypeReference(TypeReference.STRING, 1);
		var twoDims = new ArrayTypeReference(TypeReference.STRING, 2);

		assertThat(oneDim.getQualifiedName()).isEqualTo("java.lang.String[]");
		assertThat(twoDims.getQualifiedName()).isEqualTo("java.lang.String[][]");
		assertThat(oneDim).isNotEqualTo(twoDims);
	}

	@Test
	void upper_and_lower_bounded_wildcards_are_distinct() {
		var upper = new WildcardTypeReference(List.of(TypeReference.STRING), true);
		var lower = new WildcardTypeReference(List.of(TypeReference.STRING), false);

		assertThat(upper).hasToString("? extends java.lang.String");
		assertThat(lower).hasToString("? super java.lang.String");
		assertThat(upper).isNotEqualTo(lower);
	}

	@Test
	void wildcards_are_named_after_their_bounds() {
		var wildcard = new WildcardTypeReference(List.of(TypeReference.STRING), true);

		assertThat(wildcard.getQualifiedName()).isEqualTo("? extends java.lang.String");
	}
}
