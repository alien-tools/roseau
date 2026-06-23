package io.github.alien.roseau.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for the Roseau Gradle plugin.
 * <p>
 * Builds a "v1" baseline JAR, then runs the plugin against a project
 * whose source has breaking changes relative to that baseline.
 */
class RoseauPluginFunctionalTest {

    @TempDir
    Path testProjectDir;

    private Path v1Repo;

    @BeforeEach
    void setUp() throws Exception {
        // --- Build the v1 (baseline) JAR ---
        Path v1Src = testProjectDir.resolve("v1-src/pkg");
        Files.createDirectories(v1Src);

        Files.writeString(v1Src.resolve("Hello.java"), """
            package pkg;
            public class Hello {
                public void greet() { System.out.println("hello"); }
                /** @deprecated removed in v2 */
                public void legacy() { }
                public String getName() { return "Hello"; }
            }
            """);

        Files.writeString(v1Src.resolve("Util.java"), """
            package pkg;
            public class Util {
                public static int add(int a, int b) { return a + b; }
            }
            """);

        Path v1Classes = testProjectDir.resolve("v1-classes");
        Files.createDirectories(v1Classes);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null,
            "-d", v1Classes.toString(),
            v1Src.resolve("Hello.java").toString(),
            v1Src.resolve("Util.java").toString());
        assertThat(rc).isZero();

        Path v1Jar = testProjectDir.resolve("v1.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(v1Jar))) {
            addDirToJar(v1Classes, v1Classes, jos);
        }

        // Install to file-based Maven repo
        v1Repo = testProjectDir.resolve("maven-repo");
        Path artifactDir = v1Repo.resolve("test/v1-lib/1.0.0");
        Files.createDirectories(artifactDir);
        Files.copy(v1Jar, artifactDir.resolve("v1-lib-1.0.0.jar"));
        Files.writeString(artifactDir.resolve("v1-lib-1.0.0.pom"), """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>test</groupId>
              <artifactId>v1-lib</artifactId>
              <version>1.0.0</version>
            </project>
            """);

        // --- Write the test project (v2) source — has breaking changes ---
        Path src = testProjectDir.resolve("src/main/java/pkg");
        Files.createDirectories(src);

        // v2: removed legacy(), changed getName return type
        Files.writeString(src.resolve("Hello.java"), """
            package pkg;
            public class Hello {
                public void greet() { System.out.println("hello v2"); }
                public CharSequence getName() { return "Hello v2"; }
            }
            """);

        Files.writeString(src.resolve("Util.java"), """
            package pkg;
            public class Util {
                public static int add(int a, int b) { return a + b; }
            }
            """);

        Files.writeString(testProjectDir.resolve("settings.gradle.kts"), """
            rootProject.name = "roseau-test"
            """);
    }

    // -- Tests --

    @Test
    void shouldDetectBreakingChangeAndFailBuild() throws IOException {
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("java")
                id("io.github.alien-tools.roseau")
            }

            roseau {
                mvnCoord = "test:v1-lib"
                v1 = "1.0.0"
                failOnBreaking = true

                mvnRepo {
                    maven { url = uri("%s") }
                }

                reports {
                    csv("roseau.csv")
                }
            }
            """.formatted(v1Repo.toUri().toString()));

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("roseauCheck")
            .withPluginClasspath()
            .forwardOutput()
            .buildAndFail();

        assertThat(result.task(":roseauCheck").getOutcome())
            .isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput())
            .contains("EXECUTABLE_REMOVED");
        assertThat(result.getOutput())
            .contains("Breaking Changes found");
    }

    @Test
    void shouldWriteReports() throws IOException {
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("java")
                id("io.github.alien-tools.roseau")
            }

            roseau {
                mvnCoord = "test:v1-lib"
                v1 = "1.0.0"
                failOnBreaking = false
                reportsDir = layout.buildDirectory.dir("reports/roseau")

                mvnRepo {
                    maven { url = uri("%s") }
                }

                reports {
                    csv("roseau.csv")
                    html("roseau.html")
                    json("roseau.json")
                    md("roseau.md")
                }
            }
            """.formatted(v1Repo.toUri().toString()));

        GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("roseauCheck")
            .withPluginClasspath()
            .forwardOutput()
            .build();

        Path reportsDir = testProjectDir.resolve("build/reports/roseau");
        assertThat(reportsDir.resolve("roseau.csv")).exists();
        assertThat(reportsDir.resolve("roseau.html")).exists();
        assertThat(reportsDir.resolve("roseau.json")).exists();
        assertThat(reportsDir.resolve("roseau.md")).exists();

        String csv = Files.readString(reportsDir.resolve("roseau.csv"));
        assertThat(csv).contains("pkg.Hello");
        assertThat(csv).contains("EXECUTABLE_REMOVED");
    }

    @Test
    void shouldExcludeByAnnotation() throws Exception {
        // Re-create v1 where Hello is annotated @InternalApi
        Path internalDir = testProjectDir.resolve("v1-src2/internal");
        Path pkgDir = testProjectDir.resolve("v1-src2/pkg");
        Files.createDirectories(internalDir);
        Files.createDirectories(pkgDir);

        Files.writeString(internalDir.resolve("InternalApi.java"), """
            package internal;
            public @interface InternalApi {}
            """);

        Files.writeString(pkgDir.resolve("Hello.java"), """
            package pkg;
            import internal.InternalApi;
            @InternalApi
            public class Hello {
                public void greet() { }
                public void legacy() { }
                public String getName() { return "hello"; }
            }
            """);

        Files.writeString(pkgDir.resolve("Util.java"), """
            package pkg;
            public class Util {
                public static int add(int a, int b) { return a + b; }
            }
            """);

        Path v1Classes2 = testProjectDir.resolve("v1-classes2");
        Files.createDirectories(v1Classes2);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int rc = compiler.run(null, null, null,
            "-d", v1Classes2.toString(),
            internalDir.resolve("InternalApi.java").toString(),
            pkgDir.resolve("Hello.java").toString(),
            pkgDir.resolve("Util.java").toString());
        assertThat(rc).isZero();

        Path v1Jar2 = testProjectDir.resolve("v1-2.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(v1Jar2))) {
            addDirToJar(v1Classes2, v1Classes2, jos);
        }

        Path artifactDir = v1Repo.resolve("test/v1-lib/1.0.1");
        Files.createDirectories(artifactDir);
        Files.copy(v1Jar2, artifactDir.resolve("v1-lib-1.0.1.jar"));
        Files.writeString(artifactDir.resolve("v1-lib-1.0.1.pom"), """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>test</groupId>
              <artifactId>v1-lib</artifactId>
              <version>1.0.1</version>
            </project>
            """);

        // v2 source: Hello is @InternalApi and has breaking changes
        Path v2Internal = testProjectDir.resolve("src/main/java/internal");
        Path v2Pkg = testProjectDir.resolve("src/main/java/pkg");
        Files.createDirectories(v2Internal);

        Files.writeString(v2Internal.resolve("InternalApi.java"), """
            package internal;
            public @interface InternalApi {}
            """);

        Files.writeString(v2Pkg.resolve("Hello.java"), """
            package pkg;
            import internal.InternalApi;
            @InternalApi
            public class Hello {
                public void greet() { }
                public String getName() { return "hello"; }
            }
            """);

        // Build file with @InternalApi exclusion
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), """
            plugins {
                id("java")
                id("io.github.alien-tools.roseau")
            }

            roseau {
                mvnCoord = "test:v1-lib"
                v1 = "1.0.1"
                failOnBreaking = false
                reportsDir = layout.buildDirectory.dir("reports/roseau")

                mvnRepo {
                    maven { url = uri("%s") }
                }

                excludes {
                    annotation("internal.InternalApi")
                }

                reports {
                    csv("roseau.csv")
                }
            }
            """.formatted(v1Repo.toUri().toString()));

        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments("roseauCheck")
            .withPluginClasspath()
            .forwardOutput()
            .build();

        assertThat(result.task(":roseauCheck").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);

        // @InternalApi on Hello should exclude it from the report
        String csv = Files.readString(
            testProjectDir.resolve("build/reports/roseau/roseau.csv"));
        assertThat(csv).doesNotContain("pkg.Hello");
        // Util is not annotated — still appears in the header row
    }

    private static void addDirToJar(Path base, Path dir, JarOutputStream jos) throws IOException {
        try (var files = Files.list(dir)) {
            for (Path f : files.toList()) {
                if (Files.isDirectory(f)) {
                    addDirToJar(base, f, jos);
                } else {
                    String name = base.relativize(f).toString().replace('\\', '/');
                    jos.putNextEntry(new JarEntry(name));
                    Files.copy(f, jos);
                    jos.closeEntry();
                }
            }
        }
    }
}
