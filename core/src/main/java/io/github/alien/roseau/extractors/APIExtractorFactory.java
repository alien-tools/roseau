package io.github.alien.roseau.extractors;

import io.github.alien.roseau.extractors.asm.AsmAPIExtractor;
import io.github.alien.roseau.extractors.jdt.JdtAPIExtractor;
import io.github.alien.roseau.extractors.spoon.SpoonAPIExtractor;

public enum APIExtractorFactory {
	JDT,
	SPOON,
	ASM;

	public static APIExtractor newExtractor(APIExtractorFactory factory) {
		return switch (factory) {
			case JDT   -> new JdtAPIExtractor();
			case SPOON -> new SpoonAPIExtractor();
			case ASM   -> new AsmAPIExtractor();
		};
	}
}
