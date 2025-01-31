package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;
import com.github.maracas.roseau.diff.APIDiff;

import java.nio.file.Path;

public final class RoseauTool extends AbstractTool {
	public RoseauTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		APIExtractor extractor = new SpoonAPIExtractor();
		API v1 = extractor.extractAPI(v1Path);
		API v2 = extractor.extractAPI(v2Path);

		APIDiff diff = new APIDiff(v1, v2);
		var isBreaking = !diff.getBreakingChanges().isEmpty();

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult(executionTime, isBreaking);
	}
}
