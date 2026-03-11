# Quickstart

This page covers the shortest path from checkout to a first compatibility report.

!!! info
    Roseau requires Java 25.

## 1. Build the CLI

```bash
git clone https://github.com/alien-tools/roseau.git
cd roseau
./mvnw --batch-mode package
```

The packaged CLI JAR is:

```text
cli/target/roseau-cli-<version>-jar-with-dependencies.jar
```

## 2. Run a First Diff

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar
```

`--v1` and `--v2` can each be a JAR file or a source directory.

Roseau compares the library API surface, not every declaration in the input. For the exact rules, see [What Counts as API](guides/api-surface.md).

## 3. Write Report Files

`--report=FORMAT=PATH` can be repeated in a single run.

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report=CSV=reports/breaking-changes.csv \
  --report=HTML=reports/breaking-changes.html
```

## 4. Gate a Build or Script

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```
