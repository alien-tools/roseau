# Roseau

Roseau checks source-breaking and binary-breaking changes between two versions of a Java library.

Inputs can be JAR files, source trees, or one of each. The documentation site focuses on the `--diff` workflow: local checks, CI gates, configuration, reports, and Maven integration.

!!! info
    Local builds require Java 25.

## Typical Diff

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/library-v1.jar \
  --v2 path/to/library-v2.jar \
  --plain
```

## What Roseau Does

Roseau works in two steps:

1. It extracts the API surface of each input version.
2. It compares those two API models and reports the breaking changes it finds.

This distinction matters because Roseau does not diff every declaration it can parse. It compares the declarations that belong to the exposed API surface after visibility, module, and exclusion rules are applied.

## Documentation Map

<div class="grid cards" markdown>

- :material-rocket-launch: __Getting Started__

  Build Roseau and run a first compatibility check.

  [Overview](index.md)
  [Quickstart](quickstart.md)

- :material-source-branch: __Guides__

  Follow task-oriented workflows for diffing, CI, reports, configuration, and Maven.

  [Compare Two Versions](guides/compare.md)
  [Use in CI](guides/ci.md)
  [Configuration File](guides/config.md)
  [Maven Plug-in](guides/maven-plugin.md)

- :material-lightbulb-outline: __Concepts__

  Understand the API surface Roseau compares and the breaking change kinds it reports.

  [API Surface](guides/api-surface.md)
  [Breaking Change Kinds](breaking-change-kinds.md)

- :material-book-open-page-variant-outline: __Reference__

  Look up exact CLI syntax and the full Maven plug-in parameter list.

  [CLI Options](reference/cli.md)
  [Maven Plug-in Options](reference/maven-plugin.md)

</div>

## Common Starting Points

| If the goal is... | Start here |
| --- | --- |
| run a first diff locally | [Quickstart](quickstart.md) |
| compare two released JARs or two source trees | [Compare Two Versions](guides/compare.md) |
| fail CI when compatibility breaks | [Use in CI](guides/ci.md) |
| define shared settings in `roseau.yaml` | [Configuration File](guides/config.md) |
| generate HTML, JSON, CSV, or Markdown artifacts | [Report Formats](guides/reports.md) |
| understand why a declaration is or is not compared | [API Surface](guides/api-surface.md) |

## Common Commands

| Task | Command |
| --- | --- |
| Release-to-release check | `--diff --v1 library-1.0.0.jar --v2 library-1.1.0.jar` |
| Source-tree comparison | `--diff --v1 old/src --v2 new/src` |
| CI gate | `--diff --v1 v1.jar --v2 v2.jar --fail-on-bc --plain` |
| Report files | `--report=HTML=reports/report.html --report=JSON=reports/report.json` |
