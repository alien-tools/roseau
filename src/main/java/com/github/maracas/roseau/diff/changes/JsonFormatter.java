package com.github.maracas.roseau.diff.changes;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.maracas.roseau.api.model.SourceLocation;

public class JsonFormatter implements BreakinChangesFormatter {

    /**
     * Formats the list of breaking changes in JSON format
     */
    @Override
    public String format(List<BreakingChange> breakingChanges) {
        JSONArray jsonArray = new JSONArray();
        
        for (BreakingChange bc : breakingChanges) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("element", bc.impactedSymbol().getQualifiedName());

            jsonObject.put("oldPosition", createLocationJson(bc.impactedSymbol().getLocation()));

            if (bc.newSymbol() != null) {
                JSONObject newPosition = createLocationJson(bc.newSymbol().getLocation());
                jsonObject.put("newPosition", newPosition);
            } else {
                jsonObject.put("newPosition", "");
            }

            jsonObject.put("kind", bc.kind());
            jsonObject.put("nature", bc.kind().getNature());
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

    private JSONObject createLocationJson(SourceLocation location) {
		JSONObject position = new JSONObject();
		position.put("path", location.file());
		position.put("line", location.line());
		return position;
	}

}
