package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.API;

public enum ApiBuilderType implements ApiBuilder {
	ASM {
		@Override
		public API build(String sources) {
			return TestUtils.buildAsmAPI(sources);
		}
	},
	JDT {
		@Override
		public API build(String sources) {
			return TestUtils.buildJdtAPI(sources);
		}
	}
}
