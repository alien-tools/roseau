# Check Breaking Changes

Use this page to check breaking changes and compatibility between any two versions of a Java library.

Roseau can compare two JARs, two source trees, or one JAR against one source tree.

## Choose Your Input Pair

**JAR vs JAR**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/library-2.0.0.jar
```

**Source vs source**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/project-v1/src \
  --v2 path/to/project-v2/src
```

**JAR vs source**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/project-v2/src
```

## Useful Flags

- `--plain`: disable ANSI colors, useful for logs and CI
- `--binary-only`: only report binary-breaking changes
- `--source-only`: only report source-breaking changes
- `--report` with `--format`: write the report to a file
- `--classpath` or `--pom`: provide dependencies when they matter for analysis

## Common Variants

**Write an HTML report**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/report.html \
  --format HTML
```

**Only binary-breaking changes**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --binary-only \
  --plain
```

**Only source-breaking changes**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --source-only \
  --plain
```

!!! note
    `--report` requires `--format`, and `--source-only` cannot be combined with `--binary-only`.

## Next

- [Report Formats](reports.md)
- [Use in CI](ci.md)
- [CLI Options](../reference/cli.md)
