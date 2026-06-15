package io.github.alien.roseau.utils.compat;

import java.util.Locale;

enum DiffGroundTruthMode {
	OFF,
	REPORT,
	FAIL;

	static final String PROPERTY = "roseau.diff.groundTruth";

	static DiffGroundTruthMode current() {
		return switch (System.getProperty(PROPERTY, "off").toLowerCase(Locale.ROOT)) {
			case "report" -> REPORT;
			case "fail", "true" -> FAIL;
			default -> OFF;
		};
	}

	boolean isEnabled() {
		return this != OFF;
	}
}
