package io.github.alien.roseau.cli;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

record CliResult(int exitCode, String stdout, String stderr, List<LogEvent> logs) {
	// Log records at WARN or above, i.e., the ones users see by default
	List<LogEvent> warnings() {
		return logs.stream()
			.filter(event -> event.getLevel().isMoreSpecificThan(Level.WARN))
			.toList();
	}

	String describeLogs() {
		return logs.stream()
			.map(event -> "[%s] %s".formatted(event.getLevel(), event.getMessage().getFormattedMessage()))
			.collect(Collectors.joining(System.lineSeparator()));
	}

	static String normalize(String output) {
		return output.replace("\r\n", "\n").replace('\\', '/');
	}

	static String stabilize(String output) {
		var workingDirectory = normalize(Path.of("").toAbsolutePath().toString());
		return normalize(output)
			.replace(workingDirectory + "/", "")
			.replaceAll("\\(\\d+ ms\\)", "(<n> ms)")
			.replaceAll("took \\d+ms", "took <n>ms");
	}
}
