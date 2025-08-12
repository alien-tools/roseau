package io.github.alien.roseau.extractors;

import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonTypesExtractor;

public enum ExtractorType {
	SPOON,
	ASM,
	JDT;

	public TypesExtractor newExtractor() {
		return switch (this) {
			case JDT -> new JdtTypesExtractor();
			case SPOON -> new SpoonTypesExtractor();
			case ASM -> new AsmTypesExtractor();
		};
	}
}
