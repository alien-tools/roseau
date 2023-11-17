package com.github.maracas.roseau.diff;

import com.github.maracas.roseau.api.APIExtractor;
import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.support.compiler.VirtualFile;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.maracas.roseau.diff.changes.BreakingChangeKind.*;

class ClassRemovedTest {
	@Test
	void foo() {
		String v1 = "class A {}";
		String v2 = "class B {}";

		var bcs = buildDiff(v1, v2);
		assertBC("A", CLASS_REMOVED, 1, bcs);
	}

	static void assertBC(String symbol, BreakingChangeKind kind, int line , List<BreakingChange> bcs) {
		List<BreakingChange> matches = bcs.stream()
			.filter(bc ->
				   kind   == bc.kind()
				&& line   == bc.location().beginLine()
				&& symbol.equals(bc.impactedSymbol().getQualifiedName())
			)
			.toList();

		String desc = "[%s, %s, %d]".formatted(symbol, kind, line);

		if (matches.isEmpty()) {
			String found = bcs.stream()
				.filter(bc -> bc.location().beginLine() == line)
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.location().beginLine()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("No match", desc, found);
		}

		if (matches.size() > 1) {
			String found = matches.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.location().beginLine()))
				.collect(Collectors.joining(", "));

			throw new AssertionFailedError("Multiple matches", desc, found);
		}
	}

	private List<BreakingChange> buildDiff(String sourcesV1, String sourcesV2) {
		APIExtractor extractor1 = new SpoonAPIExtractor(buildModel(sourcesV1));
		APIExtractor extractor2 = new SpoonAPIExtractor(buildModel(sourcesV2));
		return new APIDiff(extractor1.extractAPI(), extractor2.extractAPI()).diff();
	}

	private CtModel buildModel(String sources) {
		Launcher launcher = new Launcher();

		launcher.addInputResource(new VirtualFile(sources));
		launcher.getEnvironment().setComplianceLevel(17);

		return launcher.buildModel();
	}
}
