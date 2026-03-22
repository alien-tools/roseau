package io.github.alien.roseau.options;

import io.github.alien.roseau.DiffPolicy;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class IgnoredCsvFile {
	private final List<DiffPolicy.IgnoredBreakingChange> ignoredBCs;

	public IgnoredCsvFile(Path csv) {
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
						return new DiffPolicy.IgnoredBreakingChange(
							fields[0].trim(),
							fields[1].trim(),
							BreakingChangeKind.valueOf(fields[2].trim())
						);
					} catch (IllegalArgumentException ignored) {
						throw new RoseauException("Malformed kind '%s' in %s".formatted(fields[2], csv));
					}
				})
				.toList();
		} catch (IOException e) {
			throw new RoseauException("Couldn't read CSV file %s".formatted(csv), e);
		}
	}

	public List<DiffPolicy.IgnoredBreakingChange> ignoredBreakingChanges() {
		return ignoredBCs;
	}
}
