package io.github.alien.roseau.cli;

import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static io.github.alien.roseau.cli.CliResultAssert.assertThatCli;
import static org.assertj.core.api.Assertions.assertThat;

final class CliRunner {
	private static final String ROSEAU_LOGGER = "io.github.alien.roseau";

	private CliRunner() {
	}

	static CliResult run(String... args) {
		var result = runExpectingDiagnostics(args);
		assertThatCli(result).hasNoStderr().loggedNoWarnings();
		return result;
	}

	static CliResult runExpectingDiagnostics(String... args) {
		var out = new StringWriter();
		var err = new StringWriter();
		var outWriter = new PrintWriter(out);
		var errWriter = new PrintWriter(err);
		var levelBefore = LogManager.getLogger(ROSEAU_LOGGER).getLevel();

		try (LogCapture logs = LogCapture.install()) {
			var cmd = new CommandLine(new RoseauCLI());
			cmd.setOut(outWriter);
			cmd.setErr(errWriter);

			var exitCode = cmd.execute(args);

			outWriter.flush();
			errWriter.flush();

			assertThat(LogManager.getLogger(ROSEAU_LOGGER).getLevel())
				.as("the CLI must restore the log level it found")
				.isEqualTo(levelBefore);

			return new CliResult(exitCode, out.toString(), err.toString(), logs.events());
		}
	}
}
