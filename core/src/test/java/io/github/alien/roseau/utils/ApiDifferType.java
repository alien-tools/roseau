package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

public enum ApiDifferType implements ApiDiffer {
	SPOON {
		@Override
		public List<BreakingChange> diff(String v1, String v2) {
			return diff(TestUtils.buildSpoonAPI(v1), TestUtils.buildSpoonAPI(v2));
		}
	},
	ASM {
		@Override
		public List<BreakingChange> diff(String v1, String v2) {
			return diff(TestUtils.buildAsmAPI(v1), TestUtils.buildAsmAPI(v2));
		}
	},
	JDT {
		@Override
		public List<BreakingChange> diff(String v1, String v2) {
			return diff(TestUtils.buildJdtAPI(v1), TestUtils.buildJdtAPI(v2));
		}
	};

	List<BreakingChange> diff(API v1, API v2) {
		return new APIDiff(v1, v2).diff().getBreakingChanges();
	}
}
