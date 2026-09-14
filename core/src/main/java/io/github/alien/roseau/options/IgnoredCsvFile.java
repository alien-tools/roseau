package io.github.alien.roseau.options;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class IgnoredCsvFile {
	private final List<Ignored> ignoredBCs;

	private record Ignored(String type, String symbol, BreakingChangeKind kind) {}

	public IgnoredCsvFile(Path csv) {
		try (Stream<String> lines = Files.lines(csv)) {
			ignoredBCs = lines
				.map(String::strip)
				.filter(line -> !line.isEmpty())
				.filter(line -> line.charAt(0) != '#')
				.filter(line -> !line.startsWith("type;symbol;kind"))
				.map(line -> {
					String[] fields = line.split(";", -1);
					if (fields.length < 3) {
						throw new RoseauException("Malformed line '%s' in %s, expecting <type>;<symbol>;<kind>"
							.formatted(line, csv));
					}
					String kind = unquote(fields[2]);
					try {
						return new Ignored(unquote(fields[0]), unquote(fields[1]), BreakingChangeKind.valueOf(kind));
					} catch (IllegalArgumentException ignored) {
						throw new RoseauException("Malformed kind '%s' in %s".formatted(kind, csv));
					}
				})
				.toList();
		} catch (IOException e) {
			throw new RoseauException("Couldn't read CSV file %s".formatted(csv), e);
		}
	}

	private static String unquote(String field) {
		String trimmed = field.trim();
		if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
			return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
		}
		return trimmed;
	}

	public boolean isIgnored(BreakingChange bc) {
		return ignoredBCs.stream().anyMatch(ign -> bc.impactedType().getQualifiedName().equals(ign.type()) &&
			bc.impactedSymbol().getQualifiedName().equals(ign.symbol()) &&
			bc.kind() == ign.kind());
	}
}
