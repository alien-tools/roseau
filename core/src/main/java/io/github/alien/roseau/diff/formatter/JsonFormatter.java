package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.changes.BreakingChange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * A formatter for {@link BreakingChange} instances that produces a JSON output.
 */
public class JsonFormatter implements BreakingChangesFormatter {
	/**
	 * Formats the list of breaking changes in JSON format
	 */
	@Override
	public String format(API api, List<BreakingChange> changes) {
		JSONArray jsonArray = new JSONArray();

		for (BreakingChange bc : changes) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("element", bc.impactedSymbol().getQualifiedName());

			jsonObject.put("oldLocation", createLocationJson(bc.impactedSymbol().getLocation()));

			if (bc.newSymbol() != null) {
				JSONObject newLocation = createLocationJson(bc.newSymbol().getLocation());
				jsonObject.put("newLocation", newLocation);
			}

			jsonObject.put("kind", bc.kind());
			jsonObject.put("nature", bc.kind().getNature());
			jsonArray.put(jsonObject);
		}
		return jsonArray.toString();
	}

	private static JSONObject createLocationJson(SourceLocation location) {
		JSONObject position = new JSONObject();
		position.put("path", location.file());
		position.put("line", location.line());
		position.put("column", location.column());
		return position;
	}
}
