package com.github.maracas.roseau.diff.formatter;

/**
 * Enumerates the possible formats for the report output. Currently supports JSON, CSV, and HTML.
 */
public enum BreakingChangesFormatterFactory {
	CSV,
	HTML,
	JSON;

	public static BreakingChangesFormatter newBreakingChangesFormatter(BreakingChangesFormatterFactory format) {
		return switch (format) {
			case JSON -> new JsonFormatter();
			case CSV  -> new CsvFormatter();
			case HTML -> new HtmlFormatter();
		};
	}
}
