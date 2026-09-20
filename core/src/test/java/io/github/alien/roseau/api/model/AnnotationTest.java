package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnnotationTest {
	static final TypeReference<AnnotationDecl> ANN = new TypeReference<>("pkg.Ann");

	@Test
	void an_annotation_without_values_has_none() {
		var ann = new Annotation(ANN);

		assertThat(ann.values()).isEmpty();
		assertThat(ann.hasValues(Map.of())).isTrue();
		assertThat(ann.hasValues(Map.of("value", "1"))).isFalse();
	}

	@Test
	void has_values_matches_any_subset_of_the_declared_values() {
		var ann = new Annotation(ANN, Map.of("value", "1", "other", "2"));

		assertThat(ann.hasValues(Map.of("value", "1"))).isTrue();
		assertThat(ann.hasValues(Map.of("other", "2"))).isTrue();
		assertThat(ann.hasValues(Map.of("value", "1", "other", "2"))).isTrue();
		assertThat(ann.hasValues(Map.of("value", "1", "other", "3"))).isFalse();
		assertThat(ann.hasValues(Map.of("value", "1", "missing", "2"))).isFalse();
	}

	@Test
	void values_are_defensively_copied() {
		var values = new HashMap<String, String>();
		values.put("value", "1");
		var ann = new Annotation(ANN, values);
		values.put("value", "2");

		assertThat(ann.values()).containsExactly(Map.entry("value", "1"));
	}

	@Test
	void empty_values_are_legal() {
		// e.g. JAXB's @XmlType(name = "") on anonymous complex types
		var ann = new Annotation(ANN, Map.of("name", ""));

		assertThat(ann.values()).containsExactly(Map.entry("name", ""));
		assertThat(ann.hasValues(Map.of("name", ""))).isTrue();
	}

	@Test
	void annotations_reject_invalid_value_maps() {
		var nullKey = new HashMap<String, String>();
		nullKey.put(null, "1");
		var nullValue = new HashMap<String, String>();
		nullValue.put("value", null);

		assertThatThrownBy(() -> new Annotation(ANN, null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new Annotation(ANN, nullKey)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Annotation(ANN, nullValue)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Annotation(ANN, Map.of("", "1"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Annotation(null, Map.of())).isInstanceOf(NullPointerException.class);
	}

}
