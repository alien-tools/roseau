package io.github.alien.roseau.git;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.diff.RoseauReport;

import java.util.List;
import java.util.Optional;

/**
 * Result of analyzing a single commit: combines git-level {@link CommitInfo} with
 * Roseau API extraction and diff results.
 *
 * <p>{@code api} is empty only before any Java commit has been processed. Once the first
 * Java commit is analyzed, all subsequent commits carry an API (even those that do not
 * touch Java — they reuse the previous API).</p>
 *
 * <p>{@code report} is empty when there is nothing to diff against (first commit) or
 * when the API was determined to be identical to the previous one.</p>
 */
public record CommitAnalysis(
	CommitInfo commit,
	Optional<API> api,
	Optional<RoseauReport> report,
	boolean apiChanged,
	long checkoutTimeMs,
	long apiTimeMs,
	long diffTimeMs,
	List<Exception> errors
) {
}
