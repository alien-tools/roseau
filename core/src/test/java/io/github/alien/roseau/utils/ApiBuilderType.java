package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.API;

public enum ApiBuilderType implements ApiBuilder {
	ASM {
		@Override
		public API build(String sources) {
			return TestUtils.buildJarAPI(sources);
		}
	},
	JDT {
		@Override
		public API build(String sources) {
			return TestUtils.buildSourcesAPI(sources);
		}
	}
}
