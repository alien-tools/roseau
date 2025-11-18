package io.github.alien.roseau.combinatorial.v2.benchmark.tool;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.factory.DefaultApiFactory;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;

import java.nio.file.Path;

public final class RoseauTool extends AbstractTool {
	public RoseauTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		var types1 = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()))
			.extractTypes(Library.of(v1Path));
		var types2 = new AsmTypesExtractor(new DefaultApiFactory(new CachingTypeReferenceFactory()))
			.extractTypes(Library.of(v2Path));

		var report = Roseau.diff(types1.toAPI(), types2.toAPI());
		var breakingChanges = report.getBreakingChanges();
		var isBinaryBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isBinaryBreaking());
		var isSourceBreaking = breakingChanges.stream().anyMatch(bC -> bC.kind().isSourceBreaking());

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Roseau", executionTime, isBinaryBreaking, isSourceBreaking);
	}
}
