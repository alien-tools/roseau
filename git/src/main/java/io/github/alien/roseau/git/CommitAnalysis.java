package io.github.alien.roseau.git;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Result of analyzing a single commit.
 */
public record CommitAnalysis(
	CommitInfo commit,
	Optional<API> api,
	Optional<RoseauReport> report,
	boolean apiChanged,
	long checkoutTimeMs,
	long apiTimeMs,
	long diffTimeMs,
	List<String> errors,
	Optional<Path> sourceRoot
) {
	public CommitAnalysis {
		errors = List.copyOf(errors);
	}
}
