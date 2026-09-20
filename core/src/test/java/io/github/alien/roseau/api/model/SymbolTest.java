package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.utils.ApiTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolTest {
	@ParameterizedTest
	@CsvSource({
		"T,                   T",
		"pkg.T,               T",
		"pkg.sub.T,           T",
		"pkg.sub.T$Inner,     Inner",
		"pkg.sub.T$In$Nested, Nested"
	})
	void simple_name_of_a_type_is_the_last_segment(String qualifiedName, String expectedSimpleName) {
		var type = ApiTestFactory.newClass(qualifiedName, AccessModifier.PUBLIC);

		assertThat(type.getSimpleName()).isEqualTo(expectedSimpleName);
	}

	@ParameterizedTest
	@CsvSource({
		"T,               T.f,               f",
		"pkg.T,           pkg.T.f,           f",
		"pkg.sub.T,       pkg.sub.T.f,       f",
		"pkg.sub.T$Inner, pkg.sub.T$Inner.f, f"
	})
	void simple_name_of_a_member_is_the_last_segment(String typeName, String qualifiedName, String expectedSimpleName) {
		var field = ApiTestFactory.newField(typeName, qualifiedName, PrimitiveTypeReference.INT, Set.of());

		assertThat(field.getSimpleName()).isEqualTo(expectedSimpleName);
	}

	@Test
	void unique_id_of_types_and_fields_is_their_qualified_name() {
		var type = ApiTestFactory.newClass("pkg.T", AccessModifier.PUBLIC);
		var field = ApiTestFactory.newField("pkg.T", "pkg.T.f", PrimitiveTypeReference.INT, Set.of());

		assertThat(type.getUniqueId()).isEqualTo("pkg.T");
		assertThat(field.getUniqueId()).isEqualTo("pkg.T.f");
	}

	@Test
	void overloads_share_a_qualified_name_but_not_a_unique_id() {
		var containing = new TypeReference<>("pkg.T");
		var m1 = ApiTestFactory.newMethod("pkg.T.m", containing, PrimitiveTypeReference.VOID,
			List.of(), Set.of());
		var m2 = ApiTestFactory.newMethod("pkg.T.m", containing, PrimitiveTypeReference.VOID,
			List.of(ApiTestFactory.newParameter("i", PrimitiveTypeReference.INT)), Set.of());

		assertThat(m1.getQualifiedName()).isEqualTo(m2.getQualifiedName());
		assertThat(m1.getUniqueId()).isEqualTo("pkg.T.m()");
		assertThat(m2.getUniqueId()).isEqualTo("pkg.T.m(int)");
		assertThat(m1).isNotEqualTo(m2);
	}

	@Test
	void annotation_lookup_matches_on_the_annotation_type() {
		var deprecated = new TypeReference<AnnotationDecl>("java.lang.Deprecated");
		var override = new TypeReference<AnnotationDecl>("java.lang.Override");
		var type = new ClassDecl("pkg.T", AccessModifier.PUBLIC, Set.of(), Set.of(new Annotation(deprecated)),
			SourceLocation.NO_LOCATION, Set.of(), List.of(), Set.of(), Set.of(), null, null, Set.of(), Set.of());

		assertThat(type.hasAnnotation(deprecated)).isTrue();
		assertThat(type.getAnnotation(deprecated)).isPresent();
		assertThat(type.hasAnnotation(override)).isFalse();
		assertThat(type.getAnnotation(override)).isEmpty();
	}

	@Test
	void annotation_lookup_with_values_matches_a_subset_of_the_actual_values() {
		var since = new TypeReference<AnnotationDecl>("pkg.Since");
		var type = new ClassDecl("pkg.T", AccessModifier.PUBLIC, Set.of(),
			Set.of(new Annotation(since, Map.of("value", "1.0", "forRemoval", "true"))), SourceLocation.NO_LOCATION,
			Set.of(), List.of(), Set.of(), Set.of(), null, null, Set.of(), Set.of());

		assertThat(type.hasAnnotation(since, Map.of())).isTrue();
		assertThat(type.hasAnnotation(since, Map.of("value", "1.0"))).isTrue();
		assertThat(type.hasAnnotation(since, Map.of("value", "1.0", "forRemoval", "true"))).isTrue();
		assertThat(type.hasAnnotation(since, Map.of("value", "2.0"))).isFalse();
		assertThat(type.hasAnnotation(since, Map.of("unknown", "1.0"))).isFalse();
	}
}
