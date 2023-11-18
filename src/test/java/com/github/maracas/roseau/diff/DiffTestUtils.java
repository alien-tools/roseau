package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.opentest4j.AssertionFailedError;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.support.compiler.VirtualFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class DiffTestUtils {
	static void assertBC(String symbol, BreakingChangeKind kind, int line, List<BreakingChange> bcs) {
		Optional<BreakingChange> matches = bcs.stream()
			.filter(bc ->
				   kind == bc.kind()
				&& line == bc.location().line()
				&& symbol.equals(bc.impactedSymbol().getQualifiedName())
			)
			.findFirst();

		if (matches.isEmpty()) {
			String desc = "[%s, %s, %d]".formatted(symbol, kind, line);
			String found = bcs.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.location().line()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("No breaking change", desc, found);
		}
	}

	static void assertNoBC(List<BreakingChange> bcs) {
		if (!bcs.isEmpty()) {
			String found = bcs.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.location().line()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
		}
	}

	static List<BreakingChange> buildDiff(String sourcesV1, String sourcesV2) {
		APIExtractor extractor1 = new SpoonAPIExtractor(buildModel(sourcesV1));
		APIExtractor extractor2 = new SpoonAPIExtractor(buildModel(sourcesV2));
		return new APIDiff(extractor1.extractAPI(), extractor2.extractAPI()).diff();
	}

	static CtModel buildModel(String sources) {
		Launcher launcher = new Launcher();

		launcher.addInputResource(new VirtualFile(sources));
		launcher.getEnvironment().setComplianceLevel(17);

		return launcher.buildModel();
	}
}
