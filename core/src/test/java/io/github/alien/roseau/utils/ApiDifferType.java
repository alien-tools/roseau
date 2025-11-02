package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.ApiDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

public enum ApiDifferType implements ApiDiffer {
	ASM {
		@Override
		public List<BreakingChange> diff(String v1, String v2) {
			return diff(TestUtils.buildJarAPI(v1), TestUtils.buildJarAPI(v2));
		}
	},
	JDT {
		@Override
		public List<BreakingChange> diff(String v1, String v2) {
			return diff(TestUtils.buildSourcesAPI(v1), TestUtils.buildSourcesAPI(v2));
		}
	};

	List<BreakingChange> diff(API v1, API v2) {
		return new ApiDiff(v1, v2).diff().getBreakingChanges();
	}
}
