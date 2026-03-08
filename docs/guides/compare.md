# Check Breaking Changes

Use this page to check breaking changes and compatibility between any two versions of a Java library.

Roseau can compare two JARs, two source trees, or one JAR against one source tree. `--v1` and `--v2` can each point to either a JAR file or a source directory.

JAR vs JAR:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/library-2.0.0.jar
```

Source vs source:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/project-v1/src \
  --v2 path/to/project-v2/src
```

JAR vs source:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/project-v2/src
```

Useful flags:

- `--plain`: disable ANSI colors, useful for logs and CI.
- `--binary-only`: only report binary-breaking changes.
- `--source-only`: only report source-breaking changes.
- `--report` with `--format`: write the report to a file.
- `--classpath` or `--pom`: provide dependencies when they matter for analysis.

Write a report:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/report.html \
  --format HTML
```

Binary-only report:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --binary-only \
  --plain
```
