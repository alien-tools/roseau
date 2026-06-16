# Comparing two versions

Roseau compares the API surface of two library versions. The inputs can be JAR files, source trees, or one of each. For the exact API-surface rules, see [API Surface](../api-surface.md), and [YAML configuration file](config.md) for configuring library-specific API exclusions. Roseau's output can be filtered to show only binary-breaking (`--binary-only`) or source-breaking (`--source-only`) changes.

## Base Command

```bash
roseau --diff --v1 <old-version> --v2 <new-version>
```

The old and new versions can be JAR files, source trees (e.g., `src/main/java`), or Maven coordinates (`groupId:artifactId:version`):

| Workflow | Example |
| --- | --- |
| JAR vs JAR | `--v1 library-1.0.0.jar --v2 library-2.0.0.jar` |
| source vs source | `--v1 project-v1/src --v2 project-v2/src` |
| JAR vs source | `--v1 library-1.0.0.jar --v2 project-v2/src` |
| Maven coordinates | `--v1 com.example:lib:1.0.0 --v2 com.example:lib:2.0.0` |

## Add Dependencies When Resolution Matters

Dependency information matters when signatures, supertypes, annotations, or generic types refer to third-party types that are not present in the analyzed inputs.

Use a shared classpath when both versions need the same dependencies:

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar \
  --classpath libs/dependency-a.jar:libs/dependency-b.jar
```

Use a shared `pom.xml` when the classpath should be derived from Maven metadata:

```bash
roseau --v1 com.example:lib:1.0.0 --v2 com.example:lib:2.0.0 \
  --pom path/to/pom.xml
```

Use per-version options when the baseline and current version do not share the same dependency set:

```bash
roseau --diff --v1 path/to/v1/src --v2 path/to/v2/src \
  --v1-classpath libs/old-dependency.jar --v2-classpath libs/new-dependency.jar
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
