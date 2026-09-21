package io.github.alien.roseau.cli;

import org.assertj.core.api.AbstractAssert;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Arrays;

import static io.github.alien.roseau.cli.CliResult.normalize;
import static org.assertj.core.api.Assertions.assertThat;

final class CliResultAssert extends AbstractAssert<CliResultAssert, CliResult> {
	private CliResultAssert(CliResult actual) {
		super(actual, CliResultAssert.class);
	}

	static CliResultAssert assertThatCli(CliResult actual) {
		return new CliResultAssert(actual);
	}

	CliResultAssert succeeded() {
		return hasExitCode(ExitCode.SUCCESS);
	}

	CliResultAssert hasBreakingChanges() {
		return hasExitCode(ExitCode.BREAKING);
	}

	CliResultAssert failed() {
		return hasExitCode(ExitCode.ERROR);
	}

	CliResultAssert hasReport(String expected) {
		return hasStdout(expected + System.lineSeparator());
	}

	CliResultAssert hasStdout(String expected) {
		isNotNull();
		assertThat(normalize(actual.stdout())).as("stdout").isEqualTo(normalize(expected));
		return this;
	}

	CliResultAssert hasNoStdout() {
		isNotNull();
		assertThat(actual.stdout()).as("stdout").isEmpty();
		return this;
	}

	CliResultAssert stdoutContains(String... expected) {
		isNotNull();
		assertThat(normalize(actual.stdout())).as("stdout").contains(normalizeAll(expected));
		return this;
	}

	CliResultAssert stdoutDoesNotContain(String... unexpected) {
		isNotNull();
		assertThat(normalize(actual.stdout())).as("stdout").doesNotContain(normalizeAll(unexpected));
		return this;
	}

	CliResultAssert hasNoStderr() {
		isNotNull();
		assertThat(actual.stderr()).as("stderr").isEmpty();
		return this;
	}

	CliResultAssert hasStderr(String expected) {
		isNotNull();
		assertThat(normalize(actual.stderr())).as("stderr").isEqualTo(normalize(expected));
		return this;
	}

	CliResultAssert reportedError(String expected) {
		isNotNull();
		assertThat(normalize(actual.stderr()).lines().findFirst().orElse(""))
			.as("first line of stderr%nfull stderr:%n%s", actual.stderr())
			.isEqualTo(normalize(expected));
		return this;
	}

	CliResultAssert stderrContains(String... expected) {
		isNotNull();
		assertThat(normalize(actual.stderr())).as("stderr").contains(normalizeAll(expected));
		return this;
	}

	CliResultAssert stderrDoesNotContain(String... unexpected) {
		isNotNull();
		assertThat(normalize(actual.stderr())).as("stderr").doesNotContain(normalizeAll(unexpected));
		return this;
	}

	CliResultAssert loggedNoWarnings() {
		isNotNull();
		assertThat(actual.warnings())
			.as("unexpected warnings%nall records:%n%s", actual.describeLogs())
			.isEmpty();
		return this;
	}

	CliResultAssert loggedWarning(String message) {
		isNotNull();
		assertThat(actual.warnings())
			.as("a warning containing '%s'%nall records:%n%s", message, actual.describeLogs())
			.anySatisfy(event -> assertThat(normalize(event.getMessage().getFormattedMessage()))
				.contains(normalize(message)));
		return this;
	}

	JSONObject stdoutAsJson() {
		isNotNull();
		String stdout = actual.stdout();
		assertThat(stdout.stripLeading())
			.as("stdout must hold nothing but a JSON object%nstdout starts with:%n%s", head(stdout))
			.startsWith("{");
		return new JSONObject(new JSONTokener(stdout));
	}

	private CliResultAssert hasExitCode(ExitCode expected) {
		isNotNull();
		assertThat(actual.exitCode())
			.as("exit code%nstdout:%n%s%nstderr:%n%s", head(actual.stdout()), actual.stderr())
			.isEqualTo(expected.code());
		return this;
	}

	private static String head(String output) {
		return output.length() <= 500 ? output : output.substring(0, 500) + "…";
	}

	private static String[] normalizeAll(String... values) {
		return Arrays.stream(values).map(CliResult::normalize).toArray(String[]::new);
	}
}
