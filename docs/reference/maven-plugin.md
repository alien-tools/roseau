# Maven plug-in options

This page is the exhaustive reference for the `roseau:check` goal configuration.
The setup guide, baseline strategies, and report examples live in [Maven plug-in](../guides/maven-plugin.md).
Most configurations only need a baseline plus an optional failure policy. The table below covers the full parameter set when a build requires more control.

## Goal

- Goal: `roseau:check`
- Default phase: `verify`
- Dependency resolution: `compile` scope

Parameters with a Maven property can be overridden on the command line with `-D<property>=<value>`.

## Top-Level Parameters

| Parameter | Property | Type                 | Default                             | Description |
| --- | --- |----------------------|-------------------------------------| --- |
| `skip` | `roseau.skip` | `boolean`            | `false`                             | Skip plug-in execution |
| `skipIfBaselineUnresolvable` | `roseau.skipIfBaselineUnresolvable` | `boolean` | `false` | Skip the check when the baseline artifact cannot be resolved from Maven repositories |
| `binaryOnly` | `roseau.binaryOnly` | `boolean`            | false                               | Report only binary-breaking changes |
| `sourceOnly` | `roseau.sourceOnly` | `boolean`            | false                               | Report only source-breaking changes |
| `failOnIncompatibility` | `roseau.failOnIncompatibility` | `boolean`            | `false`                             | Fail the build on any breaking change |
| `failOnBinaryIncompatibility` | `roseau.failOnBinaryIncompatibility` | `boolean`            | `false`                             | Fail the build on binary-breaking changes |
| `failOnSourceIncompatibility` | `roseau.failOnSourceIncompatibility` | `boolean`            | `false`                             | Fail the build on source-breaking changes |
| `baselineCoordinates` | `roseau.baselineCoordinates` | `String`             | —                                   | Baseline as `groupId:artifactId:version[:extension[:classifier]]`; takes precedence over `baselineDependency` |
| `baselineDependency` | —                    | `Dependency`         | —                                   | Baseline Maven coordinates as structured XML; use `baselineCoordinates` for CLI overrides |
| `baselineJar` | `roseau.baselineJar` | `Path`               | —                                   | Path to a baseline JAR file |
| `classpath` | — | `List<Path>`         | —                                   | Extra classpath entries shared by both versions |
| `classpathPom` | — | `Path`               | —                                   | POM used to derive the shared classpath |
| `baselineClasspath` | — | `List<Path>`         | —                                   | Extra classpath entries for the baseline only |
| `baselineClasspathPom` | — | `Path`               | —                                   | POM used to derive the baseline classpath |
| `reports` | — | `List<ReportConfig>` | —                                   | Report files to generate |
| `reportDirectory` | `roseau.reportDirectory` | `File`               | `${project.build.directory}/roseau` | Output directory for relative report paths |
| `exportBaselineApi` | `roseau.exportBaselineApi` | `Path`               | —                                   | Export the baseline API model as JSON |
| `exportCurrentApi` | `roseau.exportCurrentApi` | `Path`               | —                                   | Export the current API model as JSON |
| `configFile` | `roseau.configFile` | `Path`               | —                                   | Path to a `roseau.yaml` file |
| `verbosity` | `roseau.verbosity` | `String`             | —                                   | Logging level: `QUIET`, `NORMAL`, `VERBOSE`, or `DEBUG` |

## `reports` Entries

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `file` | `String` | Yes | Report file path; relative paths are resolved against `reportDirectory` |
| `format` | `String` | Yes | Report format: `CSV`, `HTML`, `JSON`, or `MD` |
