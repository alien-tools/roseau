package io.github.alien.roseau.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

/**
 * Entry point for the Roseau Gradle plugin.
 *
 * <p>Plugin ID: {@code io.github.alien-tools.roseau}
 *
 * <p>Registers the {@code roseau} extension and the {@code roseauCheck} task
 * on every project that applies the Java plugin. The task is wired into the
 * {@code check} lifecycle.
 */
public final class RoseauGradlePlugin implements Plugin<Project> {

    /** Extension name for the {@code roseau { … }} block. */
    static final String EXTENSION_NAME = "roseau";

    /** Task name registered by this plugin. */
    static final String TASK_NAME = "roseauCheck";

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            // Register the DSL extension
            RoseauExtension extension = project.getExtensions()
                .create(EXTENSION_NAME, RoseauExtension.class, project);

            // Register the task
            TaskProvider<RoseauTask> task = project.getTasks()
                .register(TASK_NAME, RoseauTask.class, t -> {
                    t.setGroup("verification");
                    t.setDescription(
                        "Analyzes API breaking changes between two library versions");
                    t.getExtension().set(extension);

                    // Wire the current project's JAR as an input
                    t.getCurrentJar().set(
                        project.getTasks().named("jar", Jar.class)
                            .flatMap(jarTask -> jarTask.getArchiveFile()));
                });

            // Wire into the `check` lifecycle
            project.getTasks().named("check").configure(
                check -> check.dependsOn(task));
        });
    }
}
