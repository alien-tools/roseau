package io.github.alien.roseau.cli;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

class IgnoredCsvFile {
	private final List<Ignored> ignoredBCs;

	private record Ignored(String type, String symbol, BreakingChangeKind kind) {
	}

	IgnoredCsvFile(Path csv) {
		try (Stream<String> lines = Files.lines(csv)) {
			ignoredBCs = lines
				.map(String::strip)
				.filter(line -> !line.isEmpty())
				.filter(line -> line.charAt(0) != '#')
				.filter(line -> !line.startsWith("type;symbol;kind"))
				.map(line -> line.split(";", -1))
				.map(fields -> {
					if (fields.length < 3) {
						throw new RoseauException("Malformed line '%s' in %s, expecting <type>;<symbol>;<kind>"
							.formatted(String.join(";", fields), csv));
					}
					try {
						return new Ignored(fields[0].trim(), fields[1].trim(), BreakingChangeKind.valueOf(fields[2].trim()));
					} catch (IllegalArgumentException ignored) {
						throw new RoseauException("Malformed kind '%s' in %s".formatted(fields[2], csv));
					}
				})
				.toList();
		} catch (IOException e) {
			throw new RoseauException("Couldn't read CSV file %s".formatted(csv), e);
		}
	}

	boolean isIgnored(BreakingChange bc) {
		return ignoredBCs.stream().anyMatch(ign -> bc.impactedType().getQualifiedName().equals(ign.type()) &&
			bc.impactedSymbol().getQualifiedName().equals(ign.symbol()) &&
			bc.kind() == ign.kind());
	}
}
