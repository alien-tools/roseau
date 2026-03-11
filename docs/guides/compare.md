# Compare Two Versions

Roseau compares the API surface of two library versions. The inputs can be JAR files, source trees, or one of each.

For the exact API-surface rules, see [API Surface](api-surface.md).

## Choose the Input Pair

| Input pair | Best fit |
| --- | --- |
| JAR vs JAR | release-to-release compatibility checks |
| source vs source | branch, commit, or pull-request validation |
| JAR vs source | compare a published baseline against local changes |

## Base Command

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 <old-version> \
  --v2 <new-version>
```

Then replace the inputs according to the workflow:

| Workflow | Example |
| --- | --- |
| JAR vs JAR | `--v1 library-1.0.0.jar --v2 library-2.0.0.jar` |
| source vs source | `--v1 project-v1/src --v2 project-v2/src` |
| JAR vs source | `--v1 library-1.0.0.jar --v2 project-v2/src` |

## Add Dependencies When Resolution Matters

Dependency information matters when signatures, supertypes, annotations, or generic types refer to external types that are not present in the analyzed inputs.

Use a shared classpath when both versions need the same dependencies:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --classpath libs/dependency-a.jar:libs/dependency-b.jar
```

Use a shared `pom.xml` when the classpath should be derived from Maven metadata:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --pom path/to/pom.xml
```

Use per-version options when the baseline and current version do not share the same dependency set:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --v1-classpath libs/old-dependency.jar \
  --v2-classpath libs/new-dependency.jar
```

## Common Variants

| Goal | Option |
| --- | --- |
| plain log output | `--plain` |
| only binary-breaking changes | `--binary-only` |
| only source-breaking changes | `--source-only` |
| accepted-change baseline | `--ignored accepted.csv` |
| report artifacts | `--report=FORMAT=PATH` |

!!! note
    `--binary-only` and `--source-only` are mutually exclusive.

## A Practical Reading of the Result

In most runs, the first questions are:

1. Which symbol changed?
2. Which breaking change kind was reported?
3. Is the change source-breaking, binary-breaking, or both?

The [Breaking Change Kinds](../breaking-change-kinds.md) page answers the third question. The [Report Formats](reports.md) page covers the best output format for review, automation, or accepted-change baselines.
