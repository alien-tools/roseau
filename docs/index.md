# Roseau: Java Breaking Change and Compatibility Checker

Roseau checks breaking changes and compatibility between two versions of a Java library.

It is built for release checks, CI gates, migration validation, and any workflow where a library change must not silently break downstream code. Roseau works on JAR files, source trees, or one of each.

**Typical check**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-v1.jar \
  --v2 path/to/library-v2.jar \
  --plain
```

## Why Roseau

- Detects both source-breaking and binary-breaking changes
- Compares JARs, source directories, or mixed inputs
- Fits well in CI with `--fail-on-bc`, plain output, and report files
- Produces CLI, CSV, HTML, JSON, and Markdown reports

## Start Here

<div class="grid cards" markdown>

- :material-rocket-launch: __Quickstart__

  Build Roseau and run your first compatibility check.

  [Open Quickstart](quickstart.md)

- :material-alert-outline: __Breaking Change Kinds__

  Browse every breaking change Roseau detects, with examples and impact.

  [Open the Catalog](breaking-change-kinds.md)

- :material-console: __CLI Options__

  Look up the exact meaning of each flag.

  [Open CLI Options](reference/cli.md)

- :material-source-branch: __Check Breaking Changes__

  Compare JARs, source trees, or mixed inputs.

  [Open the Guide](guides/compare.md)

- :material-puzzle: __Maven Plug-in__

  Run compatibility checks during your Maven build.

  [Open the Guide](guides/maven-plugin.md)

</div>

## Common Use Cases

**Release check**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/library-1.1.0.jar
```

**Source tree check**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/project-v1/src \
  --v2 path/to/project-v2/src
```

**CI gate**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

## Learn More

- [Maven Plug-in](guides/maven-plugin.md)
- [Use in CI](guides/ci.md)
- [Ignore Accepted Breaking Changes](guides/ignored.md)
- [Configuration File](guides/config.md)
- [Report Formats](guides/reports.md)
