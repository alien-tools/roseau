# Compare Two Versions

Roseau compares the API surface of two library versions. The inputs can be JAR files, source trees, or one of each.

For the exact API-surface rules, see [API Surface](api-surface.md).

## Choose an Input Pair

| Input pair | Typical use |
| --- | --- |
| JAR vs JAR | release-to-release compatibility checks |
| source vs source | branch, commit, or pull-request validation |
| JAR vs source | compare a published baseline against local changes |

## JAR vs JAR

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/library-2.0.0.jar
```

## Source vs Source

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/project-v1/src \
  --v2 path/to/project-v2/src
```

## JAR vs Source

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-1.0.0.jar \
  --v2 path/to/project-v2/src
```

## Add Dependencies When Needed

Roseau can require dependency types to resolve signatures, supertypes, and annotations correctly.

Use a shared classpath:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --classpath libs/dependency-a.jar:libs/dependency-b.jar
```

Or derive the classpath from a `pom.xml`:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --pom path/to/pom.xml
```

## Common Flags

| Flag | Effect |
| --- | --- |
| `--plain` | disables ANSI colors for logs and CI |
| `--binary-only` | keeps only binary-breaking changes |
| `--source-only` | keeps only source-breaking changes |
| `--ignored` | removes accepted changes listed in a CSV file |
| `--report=FORMAT=PATH` | writes one report file; repeatable |

!!! note
    `--binary-only` and `--source-only` are mutually exclusive.
