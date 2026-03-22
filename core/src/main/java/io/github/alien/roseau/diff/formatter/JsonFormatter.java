package io.github.alien.roseau.diff.formatter;

import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
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
			report.breakingChanges().stream()
				.map(bc -> {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("impactedType", bc.impactedType().getQualifiedName());
					jsonObject.put("impactedSymbol", bc.impactedSymbol().getQualifiedName());
					jsonObject.put("kind", bc.kind());
					jsonObject.put("nature", bc.kind().getNature());
					jsonObject.put("location", bc.getLocation() != SourceLocation.NO_LOCATION
						? formatLocation(bc.getLocation())
						: JSONObject.NULL);
					jsonObject.put("newSymbol", bc.newSymbol() != null
						? BreakingChange.printSymbol(bc.newSymbol())
						: JSONObject.NULL);
					jsonObject.put("binaryBreaking", bc.kind().isBinaryBreaking());
					jsonObject.put("sourceBreaking", bc.kind().isSourceBreaking());
					return jsonObject;
				})
				.toList()
		).toString(JSON_INDENT);
	}

	private static JSONObject formatLocation(SourceLocation location) {
		JSONObject position = new JSONObject();
		position.put("path", location.file());
		position.put("line", location.line() != -1 ? location.line() : JSONObject.NULL);
		return position;
	}
}
