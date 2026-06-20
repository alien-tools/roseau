package io.github.alien.roseau.gradle;

import io.github.alien.roseau.Library;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.diff.formatter.CliFormatter;
import io.github.alien.roseau.gradle.dsl.AnnotationExclusionSpec;
import io.github.alien.roseau.gradle.dsl.ExcludeExtension;
import io.github.alien.roseau.gradle.dsl.ReportSpec;
import io.github.alien.roseau.options.RoseauOptions;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves library artifacts, runs the Roseau diff engine, writes reports,
 * and fails the build when configured to do so.
 *
 * <p>This task is cacheable — when baseline coordinates and the current JAR
 * fingerprint stay the same, Gradle skips re-execution.
 */
@CacheableTask
public abstract class RoseauTask extends DefaultTask {

    // -- Extension handle --

    @Nested
    public abstract Property<RoseauExtension> getExtension();

    // -- Current project JAR --

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getCurrentJar();

    // -- Action --

    @TaskAction
    public void execute() {
        RoseauExtension ext = getExtension().get();

        // -- Parse coordinates --
        String[] coord = parseMvnCoord(ext.getMvnCoord().get());
        String groupId = coord[0];
        String artifactId = coord[1];

        // -- Resolve baseline (v1) --
        Path v1Jar = resolveArtifact(groupId, artifactId, ext.getV1().get());

        // -- Resolve or locate target (v2) --
        Path v2Jar;
        String v2Version;
        if (ext.getV2().isPresent()) {
            v2Version = ext.getV2().get();
            v2Jar = resolveArtifact(groupId, artifactId, v2Version);
        } else {
            v2Version = ext.getVersion().get();
            v2Jar = getCurrentJar().get().getAsFile().toPath();
        }

        getLogger().lifecycle("Roseau: comparing {}:{}:{} vs {}:{}:{}",
            groupId, artifactId, ext.getV1().get(),
            groupId, artifactId, v2Version);

        // -- Exclusions --
        RoseauOptions.Exclude exclude = buildExclusions(ext);

        // -- Classpath --
        List<Path> classpath = ext.getClasspath().get().stream()
            .map(Path::of)
            .filter(Files::exists)
            .toList();

        // -- Libraries --
        Library v1Library = Library.builder()
            .location(v1Jar)
            .classpath(classpath)
            .exclusions(exclude)
            .build();

        Library v2Library = Library.builder()
            .location(v2Jar)
            .classpath(classpath)
            .exclusions(exclude)
            .build();

        // -- Diff --
        RoseauReport report = Roseau.diff(v1Library, v2Library);

        // -- Filter --
        RoseauOptions.Diff diffOptions = new RoseauOptions.Diff(
            null,
            ext.getSourceOnly().get(),
            ext.getBinaryOnly().get());
        RoseauReport filtered = report.filterReport(diffOptions);

        // -- Reports --
        Path reportsDir = ext.getReportsDir().get().getAsFile().toPath();
        writeReports(filtered, ext, reportsDir);

        // -- Console summary --
        List<BreakingChange> bcs = filtered.getBreakingChanges();
        if (bcs.isEmpty()) {
            getLogger().lifecycle("Roseau: no breaking changes found between {}:{}:{} and {}:{}:{}.",
                groupId, artifactId, ext.getV1().get(), groupId, artifactId, v2Version);
        } else {
            CliFormatter formatter = new CliFormatter(CliFormatter.Mode.PLAIN);
            getLogger().warn(formatter.format(filtered));
        }

        // -- Fail checks --
        checkFailures(filtered, ext);
    }

    // -- Private helpers --

    private static String[] parseMvnCoord(String coord) {
        String[] parts = coord.split(":");
        if (parts.length != 2) {
            throw new GradleException(
                "roseau.mvnCoord must be 'groupId:artifactId', got: " + coord);
        }
        return parts;
    }

    private Path resolveArtifact(String groupId, String artifactId, String version) {
        // Allow optional classifier: "1.0.0:sources"
        String[] parts = version.split(":", 2);
        String ver = parts[0];
        String classifier = parts.length > 1 ? parts[1] : null;

        String notation = groupId + ":" + artifactId + ":" + ver;
        if (classifier != null) {
            notation = notation + ":" + classifier;
        }

        Configuration config = getProject().getConfigurations()
            .detachedConfiguration(
                getProject().getDependencies().create(notation));
        config.setTransitive(false);
        config.setDescription("Roseau artifact resolution: " + notation);

        Set<File> files = config.resolve();
        if (files.isEmpty()) {
            throw new GradleException(
                "Roseau: failed to resolve artifact " + notation);
        }

        File jar = files.stream()
            .filter(f -> f.getName().endsWith(".jar"))
            .findFirst()
            .orElseGet(() -> files.iterator().next());

        return jar.toPath();
    }

    private static RoseauOptions.Exclude buildExclusions(RoseauExtension ext) {
        ExcludeExtension excl = ext.getExcludes().getOrNull();
        if (excl == null) {
            return new RoseauOptions.Exclude(List.of(), List.of());
        }

        List<String> names = excl.getNames().getOrElse(List.of());
        List<RoseauOptions.AnnotationExclusion> annotations = new ArrayList<>();

        for (AnnotationExclusionSpec spec : excl.getAnnotations().getOrElse(List.of())) {
            annotations.add(new RoseauOptions.AnnotationExclusion(
                spec.getName().get(),
                spec.getArgs().getOrElse(Map.of())));
        }

        return new RoseauOptions.Exclude(names, annotations);
    }

    private void writeReports(RoseauReport report, RoseauExtension ext, Path reportsDir) {
        if (!ext.getReports().isPresent()) {
            return;
        }

        for (ReportSpec spec : ext.getReports().get()) {
            String format = spec.getFormat().get();
            String fileName = spec.getFile().get().getAsFile().getName();
            Path outputPath = reportsDir.resolve(fileName);

            try {
                Files.createDirectories(outputPath.getParent());
                BreakingChangesFormatterFactory fmt =
                    BreakingChangesFormatterFactory.valueOf(format.toUpperCase(Locale.ROOT));
                report.writeReport(fmt, outputPath);
                getLogger().lifecycle("Roseau: {} report written to {}", format, outputPath);
            } catch (IOException e) {
                throw new GradleException(
                    "Roseau: failed to write " + format + " report to " + outputPath, e);
            }
        }
    }

    private void checkFailures(RoseauReport report, RoseauExtension ext) {
        List<BreakingChange> bcs = report.getBreakingChanges();
        if (ext.getFailOnBreaking().get() && !bcs.isEmpty()) {
            throw new GradleException(
                "Roseau: " + bcs.size()
                    + " breaking change(s) found — build failed (failOnBreaking=true)");
        }
        if (ext.getFailOnBinaryBreaking().get() && report.isBinaryBreaking()) {
            throw new GradleException(
                "Roseau: binary-incompatible change(s) found — build failed "
                    + "(failOnBinaryBreaking=true)");
        }
        if (ext.getFailOnSourceBreaking().get() && report.isSourceBreaking()) {
            throw new GradleException(
                "Roseau: source-incompatible change(s) found — build failed "
                    + "(failOnSourceBreaking=true)");
        }
    }
}
