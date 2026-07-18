package io.github.alien.roseau.gradle.dsl;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

/**
 * Gradle model for a single annotation-based exclusion entry.
 *
 * <p>Maps to {@code io.github.alien.roseau.options.RoseauOptions.AnnotationExclusion}.
 *
 * <pre>{@code
 * annotation("org.apiguardian.api.API") {
 *     arg("status", "INTERNAL")
 * }
 * }</pre>
 */
public abstract class AnnotationExclusionSpec {

    /** Fully-qualified annotation name, e.g. {@code org.apiguardian.api.API}. */
    @Input
    public abstract Property<String> getName();

    /**
     * Annotation member-value pairs that must match for the exclusion to take effect.
     * When empty, any symbol carrying the annotation is excluded regardless of its values.
     */
    @Input
    public abstract MapProperty<String, String> getArgs();

    /** Adds a member-value pair. */
    public void arg(String key, String value) {
        getArgs().put(key, value);
    }
}
