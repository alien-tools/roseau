package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A formatter of {@link RoseauReport} that produces a JSON output.
 */
public class JsonFormatter implements BreakingChangesFormatter {
	private static final int JSON_INDENT = 2;

	/**
	 * Formats the list of breaking changes in JSON format
	 */
	@Override
	public String format(RoseauReport report) {
		return new JSONArray().putAll(
			report.getBreakingChanges().stream()
				.map(bc -> {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("impactedType", bc.impactedType().getQualifiedName());
					jsonObject.put("impactedSymbol", bc.impactedSymbol().getQualifiedName());
					jsonObject.put("kind", bc.kind());
					jsonObject.put("nature", bc.kind().getNature());
					jsonObject.put("location", formatLocation(bc.getLocation()));
					return jsonObject;
				})
				.toList()
		).toString(JSON_INDENT);
	}

	private static JSONObject formatLocation(SourceLocation location) {
		JSONObject position = new JSONObject();
		position.put("path", location.file());
		position.put("line", location.line());
		return position;
	}
}
