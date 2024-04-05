package com.github.maracas.roseau;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.api.SpoonUtils;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.SourceLocation;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;
import spoon.reflect.CtModel;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.diogonunes.jcolor.Ansi.colorize;
import static com.diogonunes.jcolor.Attribute.BOLD;
import static com.diogonunes.jcolor.Attribute.RED_TEXT;
import static com.diogonunes.jcolor.Attribute.UNDERLINE;

@CommandLine.Command(name = "roseau")
final class Roseau implements Callable<Integer>  {
	@CommandLine.Option(names = "--api",
		description = "Build and serialize the API model of --v1")
	private boolean apiMode;
	@CommandLine.Option(names = "--diff",
		description = "Compute the breaking changes between versions --v1 and --v2")
	private boolean diffMode;
	@CommandLine.Option(names = "--v1",
		description = "Path to the sources of the first version of the library", required = true)
	private Path libraryV1;
	@CommandLine.Option(names = "--v2",
		description = "Path to the sources of the second version of the library")
	private Path libraryV2;
	@CommandLine.Option(names = "--json",
		description = "Where to serialize the JSON API model of --v1; defaults to api.json",
		defaultValue = "api.json")
	private Path apiPath;
	@CommandLine.Option(names = "--report",
		description = "Where to write the breaking changes report; defaults to report.csv",
		defaultValue = "report.csv")
	private Path reportPath;
	@CommandLine.Option(names = "--verbose",
		description = "Print debug information",
		defaultValue = "false")
	private boolean verbose;
	@CommandLine.Option(names = "--fail",
			description = "Command returns an error when there are breaking changes detected",
			defaultValue = "false")
	private boolean failMode;

	private static final Logger logger = LogManager.getLogger(Roseau.class);

	private static final Duration SPOON_TIMEOUT = Duration.ofSeconds(60L);

	private static final String ROSEAU_IGNORE = ".roseau_ignore";

	private API buildAPI(Path sources) {
		Stopwatch sw = Stopwatch.createStarted();

		// Parsing
		CtModel model = SpoonUtils.buildModel(sources, SPOON_TIMEOUT);
		logger.info("Parsing {} took {}ms", sources, sw.elapsed().toMillis());
		sw.reset();
		sw.start();

		// API extraction
		SpoonAPIExtractor extractor = new SpoonAPIExtractor();
		API api = extractor.extractAPI(model);
		logger.info("Extracting API for {} took {}ms ({} types)", sources, sw.elapsed().toMillis(), api.getExportedTypes().count());

		return api;
	}

	private List<BreakingChange> diff(Path v1, Path v2, Path report) {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2));

		CompletableFuture.allOf(futureV1, futureV2).join();

		try {
			API apiV1 = futureV1.get();
			API apiV2 = futureV2.get();

			List<Pattern> ignorePatterns = List.of();
			Path config = v2.resolve(ROSEAU_IGNORE);
			if (config.toFile().exists())
				ignorePatterns = Files.readAllLines(config).stream().map(this::globToRegexp).toList();

			// API diff
			Stopwatch sw = Stopwatch.createStarted();
			APIDiff diff = new APIDiff(apiV1, apiV2, ignorePatterns);
			List<BreakingChange> bcs = diff.diff();
			logger.info("API diff took {}ms ({} breaking changes)", sw.elapsed().toMillis(), bcs.size());

			diff.writeReport(report);
			return bcs;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			logger.error("Couldn't compute diff");
		} catch (IOException e) {
			logger.error("Couldn't write diff to {}", report);
		}

		return Collections.emptyList();
	}

	private String format(BreakingChange bc) {
		return String.format("%s %s%n\t%s:%s",
			colorize(bc.kind().toString(), RED_TEXT(), BOLD()),
			colorize(bc.impactedSymbol().getQualifiedName(), UNDERLINE()),
			bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION ? "unknown" : libraryV1.toAbsolutePath().relativize(bc.impactedSymbol().getLocation().file()),
			bc.impactedSymbol().getLocation() == SourceLocation.NO_LOCATION ? "unknown" : bc.impactedSymbol().getLocation().line());
	}

	@Override
	public Integer call() {
		if (verbose) {
			Configurator.setLevel(logger, Level.INFO);
		}

		if (apiMode) {
			try {
				API api = buildAPI(libraryV1);
				api.writeJson(apiPath);
			} catch (IOException e) {
				logger.error("Couldn't write API to {}", apiPath);
			}
		}

		if (diffMode) {
			List<BreakingChange> bcs = diff(libraryV1, libraryV2, reportPath);

			System.out.println(
				bcs.stream()
					.map(this::format)
					.collect(Collectors.joining(System.lineSeparator()))
			);

			if (failMode && !bcs.isEmpty()) {
				return 1;
			}
		}

		return 0;
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new Roseau()).execute(args);
		System.exit(exitCode);
	}

	private Pattern globToRegexp(String line) {
		line = line.trim();
		int strLen = line.length();
		StringBuilder sb = new StringBuilder(strLen);
		if (line.startsWith("*")) {
			line = line.substring(1);
			strLen--;
		}
		if (line.endsWith("*")) {
			line = line.substring(0, strLen-1);
			strLen--;
		}
		boolean escaping = false;
		int inCurlies = 0;
		for (char currentChar : line.toCharArray()) {
			switch (currentChar) {
				case '*':
					if (escaping)
						sb.append("\\*");
					else
						sb.append(".*");
					escaping = false;
					break;
				case '?':
					if (escaping)
						sb.append("\\?");
					else
						sb.append('.');
					escaping = false;
					break;
				case '.':
				case '(':
				case ')':
				case '+':
				case '|':
				case '^':
				case '$':
				case '@':
				case '%':
					sb.append('\\');
					sb.append(currentChar);
					escaping = false;
					break;
				case '\\':
					if (escaping) {
						sb.append("\\\\");
						escaping = false;
					}
					else
						escaping = true;
					break;
				case '{':
					if (escaping)
						sb.append("\\{");
					else {
						sb.append('(');
						inCurlies++;
					}
					escaping = false;
					break;
				case '}':
					if (inCurlies > 0 && !escaping) {
						sb.append(')');
						inCurlies--;
					}
					else if (escaping)
						sb.append("\\}");
					else
						sb.append("}");
					escaping = false;
					break;
				case ',':
					if (inCurlies > 0 && !escaping) {
						sb.append('|');
					}
					else if (escaping)
						sb.append("\\,");
					else
						sb.append(",");
					break;
				default:
					escaping = false;
					sb.append(currentChar);
			}
		}
		return Pattern.compile(sb.toString());
	}
}
