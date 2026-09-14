package io.github.alien.roseau.gradle.dsl;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;
import java.util.ArrayList;

/**
 * Manages name-based and annotation-based exclusions.
 *
 * <pre>{@code
 * excludes {
 *     names = ["com\\.google\\.common\\..*"]
 *     annotation("com.google.common.annotations.Beta")
 *     annotation("org.apiguardian.api.API") {
 *         arg("status", "INTERNAL")
 *     }
 * }
 * }</pre>
 */
public abstract class ExcludeExtension {

    private final ObjectFactory objects;

    /** Regex patterns matching fully-qualified symbol names to exclude. */
    @Input
    public abstract ListProperty<String> getNames();

    /** Annotation-based exclusion entries. */
    @Input
    public abstract ListProperty<AnnotationExclusionSpec> getAnnotations();

    @Inject
    public ExcludeExtension(ObjectFactory objects) {
        this.objects = objects;
        getNames().convention(new ArrayList<>());
        getAnnotations().convention(new ArrayList<>());
    }

    /**
     * Registers a simple annotation exclusion (no value checks — any symbol
     * carrying the annotation is excluded).
     *
     * <pre>{@code annotation("com.google.common.annotations.Beta")}</pre>
     */
    public void annotation(String fqn) {
        AnnotationExclusionSpec spec = objects.newInstance(AnnotationExclusionSpec.class);
        spec.getName().set(fqn);
        getAnnotations().add(spec);
    }

    /**
     * Registers an annotation exclusion with member-value matching.
     *
     * <pre>{@code
     * annotation("org.apiguardian.api.API") {
     *     arg("status", "INTERNAL")
     * }
     * }</pre>
     */
    public void annotation(String fqn, Action<? super AnnotationExclusionSpec> action) {
        AnnotationExclusionSpec spec = objects.newInstance(AnnotationExclusionSpec.class);
        spec.getName().set(fqn);
        action.execute(spec);
        getAnnotations().add(spec);
    }
}
