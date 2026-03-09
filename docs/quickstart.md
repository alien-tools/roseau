# Quickstart

Use this page to build Roseau and run your first Java library compatibility check.

!!! info
    Roseau requires Java 25.

## 1. Build the CLI

```bash
git clone https://github.com/alien-tools/roseau.git
cd roseau
./mvnw --batch-mode package
```

## 2. Run a First Check

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar
```

`--v1` and `--v2` can each be a JAR file or a source directory.

Roseau compares the library API surface, not every declaration in the input. For the exact rules, see [What Counts as API](guides/api-surface.md).

## 3. Common Next Steps

**Fail CI on breaking changes**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

**Write a CSV report**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/breaking-changes.csv \
  --format CSV
```

**Write an HTML report**

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/breaking-changes.html \
  --format HTML
```

## Next

- [Check Breaking Changes](guides/compare.md)
- [What Counts as API](guides/api-surface.md)
- [Breaking Change Kinds](breaking-change-kinds.md)
- [CLI Options](reference/cli.md)
