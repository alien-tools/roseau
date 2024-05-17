package com.github.maracas.roseau.diff.formatter;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.diff.changes.BreakingChange;

public class JsonFormatter implements BreakingChangesFormatter {

    /**
     * Formats the list of breaking changes in JSON format
     */
    @Override
    public String format(List<BreakingChange> breakingChanges) {
        JSONArray jsonArray = new JSONArray();

        for (BreakingChange bc : breakingChanges) {
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

    private JSONObject createLocationJson(SourceLocation location) {
        JSONObject position = new JSONObject();
        position.put("path", location.file());
        position.put("line", location.line());
        return position;
    }

}
