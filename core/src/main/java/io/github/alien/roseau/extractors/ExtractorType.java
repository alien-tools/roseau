package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;

public enum ExtractorType {
	ASM,
	JDT;

	public TypesExtractor newExtractor(ApiFactory factory) {
		return switch (this) {
			case JDT -> new JdtTypesExtractor(factory);
			case ASM -> new AsmTypesExtractor(factory);
		};
	}
}
