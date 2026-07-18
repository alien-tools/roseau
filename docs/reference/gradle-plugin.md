# Gradle Plugin Options

The Roseau Gradle plugin (`io.github.alien-tools.roseau`) adds a `roseau` extension and a `roseauCheck` task to Java projects.

## Extension properties

### `mvnCoord`

- **Type:** `String`
- **Required**

Maven `groupId:artifactId` of the library under analysis.

```kotlin
roseau {
    mvnCoord = "com.example:my-library"
}
```

### `v1`

- **Type:** `String`
- **Required**

Version string of the baseline (old) release.

### `v2`

- **Type:** `String`
- **Default:** `project.version.toString()`

Version string of the target (new) release. When omitted the current project JAR (output of the `jar` task) is used.

### `version`

- **Type:** `String`
- **Default:** `project.version.toString()`

Current project version. Used as a display label when `v2` is omitted.

### `reportsDir`

- **Type:** `DirectoryProperty`
- **Default:** `layout.buildDirectory.dir("reports/roseau")`

Output directory for report files. Relative report paths are resolved against this directory.

### `failOnBreaking`

- **Type:** `Boolean`
- **Default:** `false`

Throws `GradleException` if any breaking change is detected.

### `failOnBinaryBreaking`

- **Type:** `Boolean`
- **Default:** `false`

Throws `GradleException` if any binary-incompatible change is detected.

### `failOnSourceBreaking`

- **Type:** `Boolean`
- **Default:** `false`

Throws `GradleException` if any source-incompatible change is detected.

### `sourceOnly`

- **Type:** `Boolean`
- **Default:** `false`

When `true`, reports only source-breaking changes (filters out binary-only changes).

### `binaryOnly`

- **Type:** `Boolean`
- **Default:** `false`

When `true`, reports only binary-breaking changes (filters out source-only changes).

### `classpath`

- **Type:** `List<String>`
- **Default:** `[]`

Additional classpath entries (JAR paths) shared by both the baseline and target libraries.

---

## `mvnRepo { }` block

Adds extra Maven repositories used to resolve baseline and target artifacts:

```kotlin
mvnRepo {
    maven { url = uri("https://internal.repo/maven/") }
    mavenLocal()
    mavenCentral()
}
```

Each call registers a repository on the Gradle project. The `roseauCheck` task resolves artifact coordinates against these repositories.

---

## `excludes { }` block

### `names`

- **Type:** `List<String>`
- **Default:** `[]`

Regex patterns matching fully-qualified symbol names to exclude from breaking change reports.

### `annotation(fqn)`

Excludes any symbol carrying the named annotation:

```kotlin
annotation("com.google.common.annotations.Beta")
```

### `annotation(fqn) { arg(key, value) }`

Excludes symbols carrying the named annotation where the specified member-value pairs also match:

```kotlin
annotation("org.apiguardian.api.API") {
    arg("status", "INTERNAL")
}
```

This maps to `RoseauOptions.AnnotationExclusion(name, Map.of("status", "INTERNAL"))` in the core API.

---

## `reports { }` block

Shorthand methods for each report format:

```kotlin
reports {
    csv("roseau.csv")   // → BreakingChangesFormatterFactory.CSV
    html("roseau.html") // → BreakingChangesFormatterFactory.HTML
    json("roseau.json") // → BreakingChangesFormatterFactory.JSON
    md("roseau.md")     // → BreakingChangesFormatterFactory.MD
    cli("roseau.txt")   // → BreakingChangesFormatterFactory.CLI (plain)
}
```

Relative paths are resolved against `reportsDir`.

---

## Task

### `roseauCheck`

- **Type:** `RoseauTask`
- **Group:** `verification`
- **Depends on:** `jar` (for the current project JAR)
- **Lifecycle:** wired into `check`

The task is cacheable: when baseline coordinates and the current JAR fingerprint stay the same, Gradle skips re-execution.

```bash
./gradlew roseauCheck
```
