package com.github.maracas.roseau.utils;

import com.github.maracas.roseau.api.model.API;

public enum ApiBuilderType implements ApiBuilder {
	SOURCES {
		@Override
		public API build(String sources) {
			return TestUtils.buildSourcesAPI(sources);
		}
	},
	JAR {
		@Override
		public API build(String sources) {
			return TestUtils.buildJarAPI(sources);
		}
	}
}
