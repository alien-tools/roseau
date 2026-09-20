package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormalTypeParameterTest {
	@Test
	void unbounded_type_parameters_are_implicitly_bounded_by_object() {
		var t = new FormalTypeParameter("T", List.of());

		assertThat(t.bounds()).containsExactly(TypeReference.OBJECT);
		assertThat(t).isEqualTo(new FormalTypeParameter("T", List.of(TypeReference.OBJECT)));
	}

	@Test
	void bounds_are_kept_in_declaration_order() {
		var bounds = List.<ITypeReference>of(new TypeReference<>("pkg.A"),
			new TypeReference<>("pkg.B"));
		var t = new FormalTypeParameter("T", bounds);

		assertThat(t.bounds()).containsExactlyElementsOf(bounds);
		assertThat(t).isNotEqualTo(new FormalTypeParameter("T", bounds.reversed()));
	}

	@Test
	void type_parameters_are_identified_by_their_name_and_bounds() {
		var t = new FormalTypeParameter("T", List.of(TypeReference.STRING));

		assertThat(t).isEqualTo(new FormalTypeParameter("T", List.of(TypeReference.STRING)));
		assertThat(t).isNotEqualTo(new FormalTypeParameter("U", List.of(TypeReference.STRING)));
		assertThat(t).isNotEqualTo(new FormalTypeParameter("T", List.of(TypeReference.OBJECT)));
	}
}
