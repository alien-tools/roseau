# Gradle

Roseau provides a dedicated Gradle plugin (`io.github.alien-tools.roseau`) that registers a `roseau` extension and a `roseauCheck` task wired into the `check` lifecycle.

## Dedicated plugin

### Applying the plugin

Buildscript classpath (Maven Central):

```kotlin title="build.gradle.kts"
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.github.alien-tools:roseau-gradle-plugin:0.7.0")
    }
}

apply(plugin = "io.github.alien-tools.roseau")
```

### Basic usage

Compare the current project JAR against a published baseline:

```kotlin title="build.gradle.kts"
roseau {
    mvnCoord = "com.example:my-library"
    v1 = "1.0.0"                        // baseline version
    // v2 omitted → uses current project JAR
    failOnBreaking = true
}
```

Run with:

```bash
./gradlew roseauCheck
```

### Comparing two published versions

```kotlin
roseau {
    mvnCoord = "com.example:my-library"
    v1 = "1.0.0"
    v2 = "2.0.0"
    failOnBreaking = true
}
```

### Annotation-based exclusions

Symbols annotated with specific annotations (and optional member values) are excluded from breaking change reports:

```kotlin
roseau {
    mvnCoord = "com.example:my-library"
    v1 = "1.0.0"

    excludes {
        // Regex patterns for symbol qualified names
        names = listOf("com\\.google\\.common\\..*")

        // Simple: any symbol carrying this annotation is ignored
        annotation("com.google.common.annotations.Beta")

        // With member-value matching: only ignore symbols where
        // @API(status = INTERNAL)
        annotation("org.apiguardian.api.API") {
            arg("status", "INTERNAL")
        }
    }
}
```

The `arg("status", "INTERNAL")` is automatically converted to the fully-qualified form `org.apiguardian.api.API$Status.INTERNAL` by Roseau's annotation matching.

### Reports

Generate reports in multiple formats:

```kotlin
roseau {
    mvnCoord = "com.example:my-library"
    v1 = "1.0.0"
    reportsDir = layout.buildDirectory.dir("reports/roseau")

    reports {
        csv("roseau.csv")
        html("roseau.html")
        json("roseau.json")
        md("CHANGELOG.md")
    }
}
```

### Extra Maven repositories

Add custom repositories to resolve baseline artifacts:

```kotlin
roseau {
    mvnCoord = "com.example:my-library"
    v1 = "1.0.0"

    mvnRepo {
        maven { url = uri("https://internal.repo/maven/") }
        mavenLocal()
        mavenCentral()
    }
}
```

### Failure control

```kotlin
roseau {
    failOnBreaking = true          // fail on any breaking change
    failOnBinaryBreaking = true    // fail only on binary-incompatible changes
    failOnSourceBreaking = true    // fail only on source-incompatible changes
    sourceOnly = false             // report source-breaking changes only
    binaryOnly = false             // report binary-breaking changes only
}
```

### Full configuration reference

| Property | Type | Default | Description |
|---|---|---|---|
| `mvnCoord` | `String` | _required_ | `groupId:artifactId` of the library |
| `v1` | `String` | _required_ | Baseline version |
| `v2` | `String` | `project.version` | Target version; omit to use current project JAR |
| `reportsDir` | `DirectoryProperty` | `build/reports/roseau` | Output directory for reports |
| `failOnBreaking` | `Boolean` | `false` | Fail build on any breaking changes |
| `failOnBinaryBreaking` | `Boolean` | `false` | Fail build on binary-incompatible changes |
| `failOnSourceBreaking` | `Boolean` | `false` | Fail build on source-incompatible changes |
| `sourceOnly` | `Boolean` | `false` | Report source-breaking changes only |
| `binaryOnly` | `Boolean` | `false` | Report binary-breaking changes only |
| `classpath` | `List<String>` | `[]` | Additional classpath JARs for v1 and v2 |
| `excludes.names` | `List<String>` | `[]` | Regex patterns for symbol names to exclude |
| `excludes.annotations` | DSL block | – | Annotation-based exclusion entries |
| `mvnRepo` | DSL block | – | Extra Maven repositories |
| `reports` | DSL block | – | Report outputs (csv, html, json, md, cli) |

---

## Without the dedicated plugin (fallback)

If you prefer not to use the dedicated plugin, Roseau can still be invoked via its CLI artifact with a `JavaExec` task:

```kotlin title="build.gradle.kts"
val roseau by configurations.creating

dependencies {
    roseau("io.github.alien-tools:roseau-cli:0.7.0")
}

tasks.register<JavaExec>("roseauCheck") {
    group = "verification"
    description = "Checks API breaking changes with Roseau"

    dependsOn(tasks.named("jar"))

    classpath = roseau
    mainClass.set("io.github.alien.roseau.cli.RoseauCLI")
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        },
    )

    doFirst {
        val currentJar = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        val reportsDir = layout.buildDirectory.dir("reports/roseau").get().asFile

        args(
            "--diff",
            "--v1", "com.example:my-library:1.2.3",
            "--v2", currentJar.absolutePath,
            "--classpath", sourceSets.main.get().compileClasspath.asPath,
            "--plain",
            "--fail-on-bc",
            "--report", "HTML=${reportsDir.resolve("report.html")}",
            "--report", "CSV=${reportsDir.resolve("report.csv")}",
        )
    }
}

tasks.named("check") {
    dependsOn("roseauCheck")
}
```

`--v1` is the baseline release, usually the latest released Maven coordinates for the artifact. `--v2` is the JAR produced by the current build.
If your project does not use the standard `jar` task for the artifact you publish, replace `jar` with the task that produces that artifact.
