package io.github.alien.roseau.utils.compat;

import io.github.alien.roseau.diff.changes.BreakingChange;

import java.nio.file.Path;
import java.util.List;

record DiffCompatibilityCase(
	String sourcesV1,
	List<Path> classpathV1,
	String sourcesV2,
	List<Path> classpathV2,
	List<BreakingChange> roseauBreakingChanges
) {
	DiffCompatibilityCase {
		classpathV1 = List.copyOf(classpathV1);
		classpathV2 = List.copyOf(classpathV2);
		roseauBreakingChanges = List.copyOf(roseauBreakingChanges);
	}

	boolean isSourceBreaking() {
		return roseauBreakingChanges.stream().anyMatch(bc -> bc.kind().isSourceBreaking());
	}

	boolean isBinaryBreaking() {
		return roseauBreakingChanges.stream().anyMatch(bc -> bc.kind().isBinaryBreaking());
	}
}
