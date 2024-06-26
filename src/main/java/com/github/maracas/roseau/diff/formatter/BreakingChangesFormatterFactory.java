package com.github.maracas.roseau.diff.formatter;

/**
 * Enumerates the possible formats for the report output. Currently supports JSON and CSV.
 */
public enum BreakingChangesFormatterFactory {
    CSV,
    HTML,
    JSON;

    public static BreakingChangesFormatter newBreakingChangesFormatter(BreakingChangesFormatterFactory format) {
        switch (format) {
            case JSON:
                return new JsonFormatter();
            case CSV:
                return new CsvFormatter();
            case HTML:
                return new HtmlFormatter();
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
}
