package io.github.alien.roseau.git;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Git-level information about a single commit: identity, metadata, and diff summary.
 */
public record CommitInfo(
	String sha,
	String shortMessage,
	Instant commitTime,
	boolean isMergeCommit,
	String parentSha,
	List<String> tags,
	String branch,
	boolean javaChanged,
	boolean pomChanged,
	int filesChanged,
	int locAdded,
	int locDeleted,
	Set<Path> updatedJavaFiles,
	Set<Path> deletedJavaFiles,
	Set<Path> createdJavaFiles
) {
	private static final Pattern CONVENTIONAL_COMMIT =
		Pattern.compile("^([a-zA-Z]+)(?:\\([^)]*\\))?(!)?:\\s+.+$");

	/**
	 * Extracts the conventional commit type tag (e.g. "feat", "fix") from the short message,
	 * or returns an empty string if the message does not follow the conventional commit format.
	 */
	public String conventionalCommitTag() {
		var matcher = CONVENTIONAL_COMMIT.matcher(shortMessage);
		return matcher.matches() ? matcher.group(1).toLowerCase(Locale.ROOT) : "";
	}

	/**
	 * Returns {@code true} if the short message uses the conventional commit breaking change
	 * indicator ({@code !} before the colon), e.g. {@code "feat!: remove legacy API"}.
	 */
	public boolean isConventionalBreakingChange() {
		var matcher = CONVENTIONAL_COMMIT.matcher(shortMessage);
		return matcher.matches() && matcher.group(2) != null;
	}
}
