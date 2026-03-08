# Quickstart

Use this page to build Roseau and run your first Java library compatibility check.

Roseau requires Java 25.

Build the CLI:

```bash
git clone https://github.com/alien-tools/roseau.git
cd roseau
./mvnw --batch-mode package
```

Run a breaking change check between two versions of a Java library:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar
```

This command compares two versions of a library and reports the breaking changes Roseau finds. `--v1` and `--v2` can each be a JAR file or a source directory.

Fail CI when breaking changes are found:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

Write a report file:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/breaking-changes.csv \
  --format CSV
```
