package io.github.alien.roseau.gradle.dsl;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

/**
 * A single report output (format + destination file).
 *
 * <p>The format String is mapped to a
 * {@code io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory} value
 * at execution time.
 */
public abstract class ReportSpec {

    /** Output file. Relative paths are resolved against {@code reportsDir}. */
    @Internal
    public abstract RegularFileProperty getFile();

    /** Report format: one of {@code CLI}, {@code CSV}, {@code HTML}, {@code JSON}, {@code MD}. */
    @Internal
    public abstract Property<String> getFormat();
}
