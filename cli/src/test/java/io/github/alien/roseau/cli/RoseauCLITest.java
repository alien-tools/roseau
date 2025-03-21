package io.github.alien.roseau.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class RoseauCLITest {
	RoseauCLI app;
	CommandLine cmd;

	@BeforeEach
	void setUp() {
		app = new RoseauCLI();
		cmd = new CommandLine(app);
	}

	@Test
	void testIt() throws Exception {
		var out = tapSystemOutNormalized(() -> {
			cmd.execute("--v1=src/test/resources/test-project-v1/src",
				"--v2=src/test/resources/test-project-v2/src",
				"--diff",
				"--plain");
		});

		assertThat(out, containsString("METHOD_REMOVED pkg.T.m"));
	}
}
