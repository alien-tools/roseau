package io.github.alien.roseau.git;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CommitInfoTest {
	private CommitInfo infoWithMessage(String message) {
		return new CommitInfo("abc123", message, Instant.EPOCH, false, "", List.of(), "main",
			false, false, 0, 0, 0, Set.of(), Set.of(), Set.of());
	}

	@ParameterizedTest
	@CsvSource({
		"'feat: add new feature',feat",
		"'fix: resolve NPE',fix",
		"'feat(core): scoped change',feat",
		"'refactor!: breaking refactor',refactor",
		"'docs: update readme',docs",
		"'test(cli): add test',test"
	})
	void conventional_commit_tag_is_extracted(String message, String expectedTag) {
		assertThat(infoWithMessage(message).conventionalCommitTag()).isEqualTo(expectedTag);
	}

	@ParameterizedTest
	@CsvSource({
		"'initial commit'",
		"'Update README.md'",
		"'Merge pull request #42'",
		"''",
	})
	void non_conventional_message_returns_empty_tag(String message) {
		assertThat(infoWithMessage(message).conventionalCommitTag()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
		"'refactor!: breaking refactor'",
		"'feat(core)!: scoped breaking change'",
		"'fix!: urgent breaking fix'",
	})
	void conventional_breaking_change_is_detected(String message) {
		assertThat(infoWithMessage(message).isConventionalBreakingChange()).isTrue();
	}

	@ParameterizedTest
	@CsvSource({
		"'feat: add new feature'",
		"'fix: resolve NPE'",
		"'feat(core): scoped change'",
		"'initial commit'",
		"''",
	})
	void non_breaking_conventional_commit_is_not_flagged(String message) {
		assertThat(infoWithMessage(message).isConventionalBreakingChange()).isFalse();
	}
}
