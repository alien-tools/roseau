package io.github.alien.roseau.gradle.dsl;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.util.ArrayList;

/**
 * Collects extra Maven repository definitions for resolving baseline/target artifacts.
 * Each call to {@code maven}, {@code mavenLocal}, or {@code mavenCentral} adds a
 * repository to the owning {@link Project} so that detached configurations used by
 * {@link io.github.alien.roseau.gradle.RoseauTask} can resolve artifacts against it.
 *
 * <pre>{@code
 * mvnRepo {
 *     maven { url = "https://internal.repo/maven/" }
 *     mavenLocal()
 *     mavenCentral()
 * }
 * }</pre>
 */
public abstract class MavenRepositoryExtension {

    private final Project project;

    /** Repository URLs for debug/tracking purposes. */
    @Internal
    public abstract ListProperty<String> getRepositoryUrls();

    @Inject
    public MavenRepositoryExtension(Project project) {
        this.project = project;
        getRepositoryUrls().convention(new ArrayList<>());
    }

    /** Adds a custom Maven repository. */
    public void maven(Action<? super MavenArtifactRepository> action) {
        MavenArtifactRepository repo = project.getRepositories().maven(action);
        getRepositoryUrls().add(repo.getUrl().toString());
    }

    /** Adds the local Maven repository ({@code ~/.m2/repository}). */
    public void mavenLocal() {
        project.getRepositories().mavenLocal();
        getRepositoryUrls().add("mavenLocal()");
    }

    /** Ensures Maven Central is available for artifact resolution. */
    public void mavenCentral() {
        project.getRepositories().mavenCentral();
        getRepositoryUrls().add("mavenCentral()");
    }
}
