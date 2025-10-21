package io.github.alien.roseau.utils;

import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.List;

public interface ApiDiffer {
	List<BreakingChange> diff(String v1, String v2);
}
