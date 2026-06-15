package io.github.alien.roseau.utils.compat;

import io.github.alien.roseau.diff.changes.BreakingChange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DiffGroundTruthRecorder {
	private static final ThreadLocal<List<DiffCompatibilityCase>> CASES = ThreadLocal.withInitial(ArrayList::new);

	private DiffGroundTruthRecorder() {
	}

	public static void record(String sourcesV1, List<Path> classpathV1,
	                          String sourcesV2, List<Path> classpathV2,
	                          List<BreakingChange> roseauBreakingChanges) {
		if (DiffGroundTruthMode.current().isEnabled()) {
			CASES.get().add(new DiffCompatibilityCase(sourcesV1, classpathV1, sourcesV2, classpathV2,
				roseauBreakingChanges));
		}
	}

	static List<DiffCompatibilityCase> takeCases() {
		List<DiffCompatibilityCase> cases = List.copyOf(CASES.get());
		CASES.remove();
		return cases;
	}

	static void clear() {
		CASES.remove();
	}
}
