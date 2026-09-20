package io.github.alien.roseau.api.model.reference;

import io.github.alien.roseau.api.model.TypeDecl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CachingTypeReferenceFactoryTest {
	TypeReferenceFactory factory;

	@BeforeEach
	void setUp() {
		factory = new CachingTypeReferenceFactory();
	}

	@Test
	void equal_references_are_shared_within_a_factory() {
		TypeReference<TypeDecl> first = factory.createTypeReference("pkg.T");
		TypeReference<TypeDecl> second = factory.createTypeReference("pkg.T", List.of());

		assertThat(first).isSameAs(second);
	}

	@Test
	void references_with_different_type_arguments_are_not_shared() {
		TypeReference<TypeDecl> raw = factory.createTypeReference("java.util.List");
		TypeReference<TypeDecl> parameterized = factory.createTypeReference("java.util.List",
			List.of(factory.createTypeReference("java.lang.String")));

		assertThat(raw).isNotEqualTo(parameterized);
	}

	@Test
	void references_of_different_kinds_are_not_confused() {
		var primitive = factory.createPrimitiveTypeReference("int");
		var typeParameter = factory.createTypeParameterReference("int");

		assertThat(primitive).isNotEqualTo(typeParameter);
	}
}
