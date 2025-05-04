package io.github.alien.roseau.extractors;

import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import io.github.alien.roseau.extractors.jdt.JdtTypesExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonTypesExtractor;

/**
 * A factory for {@link TypesExtractor} instances. Currently, supports {@link JdtTypesExtractor}, {@link AsmTypesExtractor},
 * and {@link SpoonTypesExtractor}.
 */
public enum TypesExtractorFactory {
	JDT,
	SPOON,
	ASM;

	public static TypesExtractor newExtractor(TypesExtractorFactory factory) {
		return switch (factory) {
			case JDT -> new JdtTypesExtractor();
			case SPOON -> new SpoonTypesExtractor();
			case ASM -> new AsmTypesExtractor();
		};
	}
}
