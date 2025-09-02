package io.github.alien.roseau.cli;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.diff.APIDiff;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.changes.NonBreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.MavenClasspathBuilder;
import io.github.alien.roseau.extractors.TypesExtractor;
import io.github.alien.roseau.extractors.TypesExtractorFactory;
import io.github.alien.roseau.extractors.asm.AsmTypesExtractor;
import org.json.JSONArray;
import org.json.JSONObject;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtExecutable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Main class implementing a CLI for interacting with Roseau. See {@code --help} for usage information.
 */
@CommandLine.Command(name = "roseau")
public final class RoseauCLI implements Callable<Integer> {
	@CommandLine.Option(names = "--api",
		description = "Serialize the API model of --v1; see --json")
	private boolean apiMode;
	@CommandLine.Option(names = "--diff",
		description = "Compute breaking changes between versions --v1 and --v2")
	private boolean diffMode;
	@CommandLine.Option(names = "--v1",
		description = "Path to the first version of the library; either a source directory or a JAR",
		required = true)
	private Path v1;
	@CommandLine.Option(names = "--v2",
		description = "Path to the second version of the library; either a source directory or a JAR")
	private Path v2;
	@CommandLine.Option(names = "--extractor",
		description = "API extractor to use: ${COMPLETION-CANDIDATES}",
		defaultValue = "JDT")
	private TypesExtractorFactory extractorFactory;
	@CommandLine.Option(names = "--json",
		description = "Where to serialize the JSON API model of --v1; defaults to api.json",
		defaultValue = "api.json")
	private Path apiPath;
	@CommandLine.Option(names = "--report",
		description = "Where to write the breaking changes report")
	private Path reportPath;
	@CommandLine.Option(names = "--verbose",
		description = "Print debug information")
	private boolean verbose;
	@CommandLine.Option(names = "--quiet",
		description = "Suppress warnings; only show errors")
	private boolean quiet;
	@CommandLine.Option(names = "--fail",
		description = "Return a non-zero code if breaking changes are detected")
	private boolean failMode;
	@CommandLine.Option(names = "--format",
		description = "Format of the report; possible values: ${COMPLETION-CANDIDATES}",
		defaultValue = "CSV")
	private BreakingChangesFormatterFactory format;
	@CommandLine.Option(names = "--pom",
		description = "A pom.xml file to build a classpath from")
	private Path pom;
	@CommandLine.Option(names = "--classpath",
		description = "A colon-separated list of elements to include in the classpath")
	private String classpathString;
	@CommandLine.Option(names = "--plain",
		description = "Disable ANSI colors, output plain text")
	private boolean plain;
	@CommandLine.Option(names = "--code-block",
		description = "Include full source block for old and new symbols in the output (Spoon recommended)")
	private boolean includeCodeBlock;
	@CommandLine.Option(names = "--code",
		description = "Include source code line for old and new symbols in the output")
	private boolean includeCode;
	@CommandLine.Option(names = "--all",
		description = "Show both breaking and non-breaking changes")
	private boolean showAll;
	@CommandLine.Option(names = "--stdout-format",
		description = "Stdout format: TEXT or JSON",
		defaultValue = "TEXT")
	private StdoutFormat stdoutFormat;

	private enum StdoutFormat { TEXT, JSON }

	private List<NonBreakingChange> lastNonBreakingChanges = List.of();

	private static final Logger LOGGER = LogManager.getLogger(RoseauCLI.class);
	private static final String RED_TEXT = "\u001B[31m";
	private static final String BOLD = "\u001B[1m";
	private static final String UNDERLINE = "\u001B[4m";
	private static final String RESET = "\u001B[0m";

	// Returns a display-friendly path by stripping temp/extraction prefixes and starting at a likely groupId root
	private static String displayPath(Path path) {
		if (path == null) return "";
		Path abs = path.toAbsolutePath();
		// Heuristic roots for Java packages (groupIds)
		String[] roots = new String[]{"com", "org", "io", "net", "edu", "gov", "jakarta", "javax"};
		for (int i = 0; i < abs.getNameCount(); i++) {
			String segment = abs.getName(i).toString();
			for (String root : roots) {
				if (segment.equals(root)) {
					return abs.subpath(i, abs.getNameCount()).toString();
				}
			}
		}
		return abs.toString();
	}

	private API buildAPI(Path sources, List<Path> classpath) {
		TypesExtractor extractor = sources.toString().endsWith(".jar")
			? new AsmTypesExtractor()
			: TypesExtractorFactory.newExtractor(extractorFactory);

		if (extractor.canExtract(sources)) {
			Stopwatch sw = Stopwatch.createStarted();
			API api = extractor.extractTypes(sources, classpath).toAPI(classpath);
			LOGGER.debug("Extracting API from sources {} using {} took {}ms ({} types)",
				sources, extractor.getName(), sw.elapsed().toMillis(), api.getExportedTypes().size());
			return api;
		} else {
			throw new RoseauException("Extractor %s does not support sources %s".formatted(extractor.getName(), sources));
		}
	}

	private List<BreakingChange> diff(Path v1path, Path v2path, List<Path> classpath) {
		CompletableFuture<API> futureV1 = CompletableFuture.supplyAsync(() -> buildAPI(v1path, classpath));
		CompletableFuture<API> futureV2 = CompletableFuture.supplyAsync(() -> buildAPI(v2path, classpath));

		CompletableFuture.allOf(futureV1, futureV2).join();

		try {
			API apiV1 = futureV1.get();
			API apiV2 = futureV2.get();

			// API diff
			APIDiff diff = new APIDiff(apiV1, apiV2);
			Stopwatch sw = Stopwatch.createStarted();
			List<BreakingChange> bcs = diff.diff();
			LOGGER.debug("API diff took {}ms ({} breaking changes)", sw.elapsed().toMillis(), bcs.size());

			if (showAll) {
				lastNonBreakingChanges = diff.getNonBreakingChanges();
				if (stdoutFormat == StdoutFormat.TEXT) {
					if (!lastNonBreakingChanges.isEmpty()) {
						System.out.println((plain ? "Non-breaking changes:" : BOLD + "Non-breaking changes:" + RESET));
						System.out.println(lastNonBreakingChanges.stream().map(this::formatNonBreaking).collect(Collectors.joining(System.lineSeparator())));
					}
				}
			}

			writeReport(apiV1, bcs);
			return bcs;
		} catch (InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Couldn't compute diff", e);
		}

		return Collections.emptyList();
	}

	private List<Path> buildClasspath() {
		List<Path> classpath = new ArrayList<>();
		if (pom != null && Files.isRegularFile(pom)) {
			Stopwatch sw = Stopwatch.createStarted();
			MavenClasspathBuilder classpathBuilder = new MavenClasspathBuilder();
			classpath.addAll(classpathBuilder.buildClasspath(pom));
			LOGGER.debug("Extracting classpath from {} took {}ms", pom, sw.elapsed().toMillis());
		}

		if (!Strings.isNullOrEmpty(classpathString)) {
			classpath.addAll(Arrays.stream(classpathString.split(":"))
				.map(Path::of)
				.toList());
		}

		if (classpath.isEmpty()) {
			LOGGER.warn("No classpath provided, results may be inaccurate");
		} else {
			LOGGER.debug("Classpath: {}", classpath);
		}

		return classpath;
	}

	private void writeReport(API api, List<BreakingChange> bcs) {
		if (reportPath == null)
			return;

		BreakingChangesFormatter fmt = BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);

		try {
			Files.writeString(reportPath, fmt.format(api, bcs));
			LOGGER.info("Wrote report to {}", reportPath);
		} catch (IOException e) {
			LOGGER.error("Couldn't write report to {}", reportPath, e);
		}
	}

	private String format(BreakingChange bc) {
		if (plain) {
			String base = String.format("%s %s%n\t%s:%s:%s", bc.kind(), bc.impactedSymbol().getQualifiedName(),
				displayPath(bc.impactedSymbol().getLocation().file()), bc.impactedSymbol().getLocation().line(), bc.impactedSymbol().getLocation().column());
			if (includeCodeBlock) {
				String oldBlock = readSourceBlock(bc.impactedSymbol().getLocation());
				String newBlock = bc.newSymbol() != null ? readSourceBlock(bc.newSymbol().getLocation()) : null;
				StringBuilder sb = new StringBuilder(base);
				if (oldBlock != null) sb.append(String.format("%n\told:%n%s", indentBlock(oldBlock)));
				if (newBlock != null) sb.append(String.format("%n\tnew:%n%s", indentBlock(newBlock)));
				return sb.toString();
			}
			if (includeCode) {
				String oldLine = readSourceLine(bc.impactedSymbol().getLocation());
				String newLine = bc.newSymbol() != null ? readSourceLine(bc.newSymbol().getLocation()) : null;
				StringBuilder sb = new StringBuilder(base);
				if (oldLine != null) sb.append(String.format("%n\told: %s", oldLine));
				if (newLine != null) sb.append(String.format("%n\tnew: %s", newLine));
				return sb.toString();
			}
			return base;
		} else {
			String base = String.format("%s %s%n\t%s:%s:%s",
				RED_TEXT + BOLD + bc.kind() + RESET,
				UNDERLINE + bc.impactedSymbol().getQualifiedName() + RESET,
				displayPath(bc.impactedSymbol().getLocation().file()), bc.impactedSymbol().getLocation().line(), bc.impactedSymbol().getLocation().column());
			if (includeCodeBlock) {
				String oldBlock = readSourceBlock(bc.impactedSymbol().getLocation());
				String newBlock = bc.newSymbol() != null ? readSourceBlock(bc.newSymbol().getLocation()) : null;
				StringBuilder sb = new StringBuilder(base);
				if (oldBlock != null) sb.append(String.format("%n\t%s%n%s", BOLD + "old:" + RESET, indentBlock(oldBlock)));
				if (newBlock != null) sb.append(String.format("%n\t%s%n%s", BOLD + "new:" + RESET, indentBlock(newBlock)));
				return sb.toString();
			}
			if (includeCode) {
				String oldLine = readSourceLine(bc.impactedSymbol().getLocation());
				String newLine = bc.newSymbol() != null ? readSourceLine(bc.newSymbol().getLocation()) : null;
				StringBuilder sb = new StringBuilder(base);
				if (oldLine != null) sb.append(String.format("%n\t%s", BOLD + "old:" + RESET + " " + oldLine));
				if (newLine != null) sb.append(String.format("%n\t%s", BOLD + "new:" + RESET + " " + newLine));
				return sb.toString();
			}
			return base;
		}
	}

	private String formatNonBreaking(NonBreakingChange nbc) {
		String title = nbc.kind() + " " + (nbc.newSymbol() != null ? nbc.newSymbol().getQualifiedName() : nbc.impactedSymbol() != null ? nbc.impactedSymbol().getQualifiedName() : "");
		var symbol = nbc.newSymbol() != null ? nbc.newSymbol() : nbc.impactedSymbol();
		if (symbol == null) return title;
		String base = String.format("%s%n\t%s:%s:%s",
			title,
			displayPath(symbol.getLocation().file()), symbol.getLocation().line(), symbol.getLocation().column());
		if (includeCodeBlock) {
			String block = readSourceBlock(symbol.getLocation());
			if (block != null) return base + String.format("%n\tcode:%n%s", indentBlock(block));
		}
		if (includeCode) {
			String line = readSourceLine(symbol.getLocation());
			if (line != null) return base + String.format("%n\tcode: %s", line);
		}
		return base;
	}

	private static String indentBlock(String block) {
		return block.lines().map(l -> "\t\t" + l).collect(Collectors.joining(System.lineSeparator()));
	}

	private String readSourceLine(SourceLocation location) {
		try {
			if (location == null || location.line() <= 0 || location.file() == null) return null;
			List<String> lines = Files.readAllLines(location.file());
			int idx = location.line() - 1;
			if (idx >= 0 && idx < lines.size()) {
				return lines.get(idx).stripTrailing();
			}
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	private String readSourceBlock(SourceLocation location) {
		try {
			if (location == null || location.file() == null || location.line() <= 0)
				return null;

			// Build a small Spoon model for this file only
			Launcher launcher = new Launcher();
			launcher.getEnvironment().setNoClasspath(true);
			launcher.addInputResource(location.file().toString());
			CtModel model = launcher.buildModel();

			int line = location.line();
			int column = Math.max(1, location.column());

			CtElement best = null;
			int bestSpan = Integer.MAX_VALUE;
			for (CtElement el : model.getAllTypes()) {
				best = findBestElement(el, location.file(), line, column, best, bestSpan);
				if (best != null) {
					SourcePosition p = best.getPosition();
					bestSpan = p.getSourceEnd() - p.getSourceStart();
				}
			}

			if (best == null)
				return null;

			// For inner-most element, climb to the nearest declaration kind we care about
			CtElement decl = best;
			while (decl != null && !(decl instanceof CtType || decl instanceof CtField || decl instanceof CtExecutable)) {
				decl = decl.getParent();
			}
			if (decl == null)
				decl = best;

			SourcePosition pos = decl.getPosition();
			if (pos == null || !pos.isValidPosition())
				return null;

			String content = Files.readString(location.file());
			int start = Math.max(0, pos.getSourceStart());
			int end = Math.min(content.length(), pos.getSourceEnd() + 1);
			return content.substring(start, end).stripTrailing();
		} catch (Exception e) {
			return null;
		}
	}

	private CtElement findBestElement(CtElement el, Path file, int line, int column, CtElement currentBest, int currentBestSpan) {
		SourcePosition p = el.getPosition();
		if (p != null && p.isValidPosition() && p.getFile() != null && file.equals(p.getFile().toPath())) {
			boolean contains = false;
			int startLine = Math.max(1, p.getLine());
			int endLine = Math.max(startLine, p.getEndLine());
			int startCol = Math.max(1, p.getColumn());
			int endCol = Math.max(startCol, p.getEndColumn());
			contains = (line > startLine || (line == startLine && column >= startCol)) &&
				(line < endLine || (line == endLine && column <= endCol));
			if (contains) {
				int span = p.getSourceEnd() - p.getSourceStart();
				if (span >= 0 && span < currentBestSpan) {
					currentBest = el;
					currentBestSpan = span;
				}
			}
		}
		for (CtElement child : el.getDirectChildren()) {
			currentBest = findBestElement(child, file, line, column, currentBest, currentBestSpan);
			if (currentBest != null) {
				currentBestSpan = currentBest.getPosition() != null ? (currentBest.getPosition().getSourceEnd() - currentBest.getPosition().getSourceStart()) : currentBestSpan;
			}
		}
		return currentBest;
	}

	private String toJsonOutput(List<BreakingChange> bcs, List<NonBreakingChange> nbcs) {
		JSONObject root = new JSONObject();
		JSONArray breaking = new JSONArray();
		for (BreakingChange bc : bcs) {
			JSONObject obj = new JSONObject();
			obj.put("kind", bc.kind().toString());
			obj.put("element", bc.impactedSymbol().getQualifiedName());
			obj.put("oldLocation", createLocationJson(bc.impactedSymbol().getLocation()));
			if (bc.newSymbol() != null) {
				obj.put("newLocation", createLocationJson(bc.newSymbol().getLocation()));
			}
			if (includeCode) {
				String oldLine = readSourceLine(bc.impactedSymbol().getLocation());
				String newLine = bc.newSymbol() != null ? readSourceLine(bc.newSymbol().getLocation()) : null;
				if (oldLine != null) obj.put("oldCode", oldLine);
				if (newLine != null) obj.put("newCode", newLine);
			}
			if (includeCodeBlock) {
				String oldBlock = readSourceBlock(bc.impactedSymbol().getLocation());
				String newBlock = bc.newSymbol() != null ? readSourceBlock(bc.newSymbol().getLocation()) : null;
				if (oldBlock != null) obj.put("oldBlock", oldBlock);
				if (newBlock != null) obj.put("newBlock", newBlock);
			}
			breaking.put(obj);
		}
		root.put("breaking", breaking);

		if (showAll && nbcs != null) {
			JSONArray nonBreaking = new JSONArray();
			for (NonBreakingChange nbc : nbcs) {
				JSONObject obj = new JSONObject();
				obj.put("kind", nbc.kind().toString());
				if (nbc.newSymbol() != null) {
					obj.put("element", nbc.newSymbol().getQualifiedName());
					obj.put("location", createLocationJson(nbc.newSymbol().getLocation()));
					if (includeCode) {
						String line = readSourceLine(nbc.newSymbol().getLocation());
						if (line != null) obj.put("code", line);
					}
					if (includeCodeBlock) {
						String block = readSourceBlock(nbc.newSymbol().getLocation());
						if (block != null) obj.put("block", block);
					}
				} else if (nbc.impactedSymbol() != null) {
					obj.put("element", nbc.impactedSymbol().getQualifiedName());
					obj.put("location", createLocationJson(nbc.impactedSymbol().getLocation()));
				}
				nonBreaking.put(obj);
			}
			root.put("nonBreaking", nonBreaking);
		}

		return root.toString();
	}

	private static JSONObject createLocationJson(SourceLocation location) {
		JSONObject position = new JSONObject();
		position.put("path", displayPath(location.file()));
		position.put("line", location.line());
		position.put("column", location.column());
		return position;
	}

	private void checkArguments() {
		if (v1 == null || !Files.exists(v1)) {
			throw new IllegalArgumentException("--v1 does not exist");
		}

		if (diffMode && (v2 == null || !Files.exists(v2))) {
			throw new IllegalArgumentException("--v2 does not exist");
		}

		if (pom != null && !Files.exists(pom)) {
			throw new IllegalArgumentException("--pom does not exist");
		}
	}

	@Override
	public Integer call() {
		if (quiet) {
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.ERROR);
		} else if (verbose) {
			Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
		}

		try {
			checkArguments();
			List<Path> classpath = buildClasspath();

			if (apiMode) {
				API api = buildAPI(v1, classpath);
				api.getLibraryTypes().writeJson(apiPath);
				LOGGER.info("Wrote API to {}", apiPath);
			}

			if (diffMode) {
				List<BreakingChange> bcs = diff(v1, v2, classpath);

				if (stdoutFormat == StdoutFormat.JSON) {
					System.out.println(toJsonOutput(bcs, lastNonBreakingChanges));
				} else {
					if (bcs.isEmpty()) {
						System.out.println("No breaking changes found.");
					} else {
						System.out.println(
							bcs.stream()
								.map(this::format)
								.collect(Collectors.joining(System.lineSeparator()))
						);
					}
				}

				if (failMode && !bcs.isEmpty()) {
					return 1;
				}
			}

			return 0;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return 1;
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new RoseauCLI()).execute(args);
		System.exit(exitCode);
	}
}
