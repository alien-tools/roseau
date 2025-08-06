package io.github.alien.roseau.combinatorial;

import io.github.alien.roseau.combinatorial.mode.CLIMode;
import io.github.alien.roseau.combinatorial.mode.CombinatorialBenchmark;
import io.github.alien.roseau.combinatorial.mode.GenerateClient;
import io.github.alien.roseau.combinatorial.utils.ExplorerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "combi", mixinStandardHelpOptions = true, version = "Combinatorial - Roseau 0.3.0")
public final class CombinatorialCLI implements Callable<Integer> {
	@CommandLine.Option(names = { "-m", "--mode" },
		description = "Mode of operation: ${COMPLETION-CANDIDATES}; defaults to BENCH",
		defaultValue = "BENCH")
	private CLIMode mode;

	@CommandLine.Option(names = { "-t", "--threads" },
		description = "Number of threads to use; only for BENCH mode; defaults to the number of available processors",
		arity = "1..*",
		defaultValue = "0")
	private int threads;

	@CommandLine.Option(names = { "-s", "--skip-previous-v2-failures" },
		description = "Skip all cases either impossible or in error when generating new api from the previous benchmark run; only for BENCH mode; defaults to false",
		defaultValue = "false")
	private boolean skipPreviousV2Failures;

	@CommandLine.Option(names = { "--api" },
		description = "Path to the API sources to use for the client generation; only and mandatory for CLIENT mode")
	private Path apiPath;

	@CommandLine.Option(names = { "-o", "--output" },
		description = "Output directory for all generated artifacts; defaults to 'output'",
		defaultValue = Constants.OUTPUT_FOLDER)
	private Path outputPath;

	@CommandLine.Option(names = { "--tmp-output" },
		description = "Output directory for the temporary files; defaults to 'tmp'",
		defaultValue = Constants.TMP_FOLDER)
	private Path tmpOutputPath;

	@CommandLine.Option(names = { "--remove-tmp" },
		description = "Remove temporary files; defaults to false",
		defaultValue = "false")
	private boolean removeTmp;

	private static final Logger LOGGER = LogManager.getLogger(CombinatorialCLI.class);

	private void checkArguments() {
		if (mode == CLIMode.BENCH) {
			var availableProcessors = Runtime.getRuntime().availableProcessors();

			if (threads < 1 || threads > availableProcessors) {
				threads = availableProcessors;
				LOGGER.info("Using available processors: {}", availableProcessors);
			}
		}

		if (mode == CLIMode.CLIENT) {
			if (apiPath == null) {
				throw new IllegalArgumentException("--api is required in CLIENTS mode");
			}

			if (!ExplorerUtils.checkPathExists(apiPath)) {
				throw new IllegalArgumentException("--api does not exist: " + apiPath);
			}
		}
	}

	@Override
	public Integer call() {
		try {
			checkArguments();

			if (mode == CLIMode.BENCH) {
				new CombinatorialBenchmark(threads, skipPreviousV2Failures, outputPath, tmpOutputPath).run();
			} else if (mode == CLIMode.CLIENT) {
				new GenerateClient(apiPath, outputPath, tmpOutputPath).run();
			}

			if (removeTmp) {
				var isRemoved = ExplorerUtils.removeDirectory(tmpOutputPath);
				if (!isRemoved) {
					LOGGER.warn("Failed to remove temporary output directory: {}", tmpOutputPath);
				}
			}

			return 0;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return 1;
		}
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new CombinatorialCLI()).execute(args));
	}
}
