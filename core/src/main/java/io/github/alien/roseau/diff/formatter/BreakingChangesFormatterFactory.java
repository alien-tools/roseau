package io.github.alien.roseau.diff.formatter;

/**
 * A factory of {@link BreakingChangesFormatter} given an expected output format. Currently, supports CSV, HTML, and
 * JSON.
 */
public enum BreakingChangesFormatterFactory {
	CSV,
	HTML,
	JSON;

	/**
	 * Returns a {@link BreakingChangesFormatter} for the supplied format.
	 *
	 * @param format the expected output format
	 * @return the corresponding formatter
	 */
	public static BreakingChangesFormatter newBreakingChangesFormatter(BreakingChangesFormatterFactory format) {
		return switch (format) {
			case JSON -> new JsonFormatter();
			case CSV -> new CsvFormatter();
			case HTML -> new HtmlFormatter();
		};
	}
}
