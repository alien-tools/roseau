package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.API;

import java.util.List;

public enum ApiBuilderType implements ApiBuilder {
	ASM {
		@Override
		public API build(String sources) {
			return TestUtils.buildJarAPI(sources);
		}

		@Override
		public API buildIgnoringModule(String sources) {
			return TestUtils.buildJarAPI(sources, true);
		}
	},
	JDT {
		@Override
		public API build(String sources) {
			return TestUtils.buildSourcesAPI(sources);
		}

		@Override
		public API buildIgnoringModule(String sources) {
			return TestUtils.buildSourcesAPI(sources, List.of(), true);
		}
	}
}
