package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFormatterTest {
	private static final List<String> KEYS = List.of("impactedType", "impactedSymbol", "kind", "nature", "location",
		"newSymbol", "binaryBreaking", "sourceBreaking");

	private static JSONArray json(RoseauReport report) {
		return new JSONArray(new JsonFormatter().format(report));
	}

	@Test
	void every_change_carries_exactly_the_documented_keys() {
		JSONArray changes = json(ReportFixtures.mixed());

		assertThat(changes).hasSize(6);
		for (int i = 0; i < changes.length(); i++) {
			assertThat(changes.getJSONObject(i).keySet()).containsExactlyInAnyOrderElementsOf(KEYS);
		}
	}

	@Test
	void changes_are_serialized_in_report_order_with_their_values() {
		JSONArray changes = json(ReportFixtures.mixed());

		assertThat(kinds(changes)).containsExactly("FORMAL_TYPE_PARAMETER_REMOVED",
			"EXECUTABLE_PARAMETER_GENERICS_CHANGED", "METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE", "METHOD_NOW_STATIC",
			"METHOD_OVERRIDABLE_NOW_STATIC", "EXECUTABLE_REMOVED");

		JSONObject removal = changes.getJSONObject(5);
		assertThat(removal.getString("impactedType")).isEqualTo("pkg.A");
		assertThat(removal.getString("impactedSymbol")).isEqualTo("pkg.A.removed()");
		assertThat(removal.getString("nature")).isEqualTo("DELETION");
		assertThat(removal.getBoolean("binaryBreaking")).isTrue();
		assertThat(removal.getBoolean("sourceBreaking")).isTrue();
	}

	@Test
	void missing_new_symbols_are_json_null() {
		JSONObject removal = json(ReportFixtures.mixed()).getJSONObject(5);

		assertThat(removal.isNull("newSymbol")).isTrue();
		assertThat(removal.get("newSymbol")).isEqualTo(JSONObject.NULL);
	}

	@Test
	void locations_are_split_into_a_path_and_a_line() {
		JSONObject location = json(ReportFixtures.mixed()).getJSONObject(5).getJSONObject("location");

		assertThat(ReportFixtures.normalize(location.getString("path"))).isEqualTo("pkg/A.java");
		assertThat(location.getInt("line")).isEqualTo(4);
	}

	@Test
	void unknown_locations_are_json_null() {
		JSONObject change = json(ReportFixtures.reportedAt(SourceLocation.NO_LOCATION)).getJSONObject(0);

		assertThat(change.isNull("location")).isTrue();
	}

	@Test
	void empty_report_is_an_empty_array() {
		assertThat(new JsonFormatter().format(ReportFixtures.empty())).isEqualTo("[]");
	}

	private static List<String> kinds(JSONArray changes) {
		return IntStream.range(0, changes.length())
			.mapToObj(i -> changes.getJSONObject(i).getString("kind"))
			.toList();
	}
}
