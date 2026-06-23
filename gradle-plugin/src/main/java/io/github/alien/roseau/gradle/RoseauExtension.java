package io.github.alien.roseau.gradle;

import io.github.alien.roseau.gradle.dsl.ExcludeExtension;
import io.github.alien.roseau.gradle.dsl.MavenRepositoryExtension;
import io.github.alien.roseau.gradle.dsl.ReportSpec;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;

import javax.inject.Inject;
import java.util.ArrayList;

/**
 * Root DSL extension registered under the {@code roseau} name.
 *
 * <pre>{@code
 * roseau {
 *     mvnCoord = "com.example:my-library"
 *     v1 = "1.0.0"
 *     // v2 defaults to the current project's JAR
 *
 *     failOnBreaking = true
 *     reportsDir = layout.buildDirectory.dir("reports/roseau")
 *
 *     excludes {
 *         names = ["com\\.google\\.common\\..*"]
 *         annotation("com.google.common.annotations.Beta")
 *         annotation("org.apiguardian.api.API") {
 *             arg("status", "INTERNAL")
 *         }
 *     }
 *
 *     reports {
 *         csv("roseau.csv")
 *         html("roseau.html")
 *     }
 * }
 * }</pre>
 */
public abstract class RoseauExtension {

    private final Project project;
    private final ObjectFactory objects;

    // -- Artifact identification --

    /** Current project/library version. Defaults to {@code project.version.toString()}. */
    @Input
    public abstract Property<String> getVersion();

    /** Maven {@code groupId:artifactId} of the library under analysis. */
    @Input
    public abstract Property<String> getMvnCoord();

    /** Baseline version string. */
    @Input
    public abstract Property<String> getV1();

    /**
     * Target version string. When omitted, the current project JAR
     * (the {@code jar} task output) is used.
     */
    @Input
    @Optional
    public abstract Property<String> getV2();

    // -- Directories --

    /** Reports output directory. Defaults to {@code build/reports/roseau}. */
    @OutputDirectory
    public abstract DirectoryProperty getReportsDir();

    // -- Failure behaviour --

    /** Fail the build when any breaking change is detected. */
    @Input
    public abstract Property<Boolean> getFailOnBreaking();

    /** Fail the build when binary-incompatible changes are found. */
    @Input
    public abstract Property<Boolean> getFailOnBinaryBreaking();

    /** Fail the build when source-incompatible changes are found. */
    @Input
    public abstract Property<Boolean> getFailOnSourceBreaking();

    // -- Diff filtering --

    /** Report source-breaking changes only. */
    @Input
    public abstract Property<Boolean> getSourceOnly();

    /** Report binary-breaking changes only. */
    @Input
    public abstract Property<Boolean> getBinaryOnly();

    // -- Classpath --

    /** Additional classpath JAR paths shared by baseline and target. */
    @Input
    @Optional
    public abstract ListProperty<String> getClasspath();

    // -- Nested extensions --

    /** Extra Maven repositories for artifact resolution. */
    @Nested
    public abstract Property<MavenRepositoryExtension> getMvnRepoExtension();

    /** Name and annotation exclusions. */
    @Nested
    @Optional
    public abstract Property<ExcludeExtension> getExcludes();

    /** Configured report outputs. */
    @Nested
    @Optional
    public abstract ListProperty<ReportSpec> getReports();

    // -- Project handle (not an input, just a back-reference) --

    @Internal
    public Project getProject() {
        return project;
    }

    @Inject
    public RoseauExtension(Project project) {
        this.project = project;
        this.objects = project.getObjects();

        getVersion().convention(project.getVersion().toString());
        getFailOnBreaking().convention(false);
        getFailOnBinaryBreaking().convention(false);
        getFailOnSourceBreaking().convention(false);
        getSourceOnly().convention(false);
        getBinaryOnly().convention(false);
        getClasspath().convention(new ArrayList<>());
        getReportsDir().convention(
            project.getLayout().getBuildDirectory().dir("reports/roseau"));
        getMvnRepoExtension().convention(
            objects.newInstance(MavenRepositoryExtension.class));
    }

    // -- DSL block methods --

    /** Configures extra Maven repositories. */
    public void mvnRepo(Action<MavenRepositoryExtension> action) {
        action.execute(getMvnRepoExtension().get());
    }

    /** Configures exclusions (name regexes + annotations). */
    public void excludes(Action<ExcludeExtension> action) {
        ExcludeExtension ext = getExcludes().getOrElse(
            objects.newInstance(ExcludeExtension.class));
        action.execute(ext);
        getExcludes().set(ext);
    }

    /** Configures report outputs. */
    public void reports(Action<ReportDsl> action) {
        ReportDsl dsl = new ReportDsl(project, objects, getReports());
        action.execute(dsl);
        getReports().set(dsl.collect());
    }

    /**
     * Internal DSL helper that exposes shorthand methods for each report format.
     */
    public static class ReportDsl {
        private final Project project;
        private final ObjectFactory objects;
        private final java.util.List<ReportSpec> specs = new ArrayList<>();

        ReportDsl(Project project, ObjectFactory objects, ListProperty<ReportSpec> existing) {
            this.project = project;
            this.objects = objects;
            if (existing.isPresent()) {
                this.specs.addAll(existing.get());
            }
        }

        public void csv(String path)  { add(path, "CSV"); }
        public void html(String path) { add(path, "HTML"); }
        public void json(String path) { add(path, "JSON"); }
        public void md(String path)   { add(path, "MD"); }
        public void cli(String path)  { add(path, "CLI"); }

        private void add(String path, String format) {
            ReportSpec spec = objects.newInstance(ReportSpec.class);
            spec.getFile().set(
                project.getLayout().getProjectDirectory().file(path));
            spec.getFormat().set(format);
            specs.add(spec);
        }

        java.util.List<ReportSpec> collect() {
            return java.util.List.copyOf(specs);
        }
    }
}
