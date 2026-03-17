# Roseau

Roseau (/ʁozo/) is a **fast** and **accurate** tool for detecting breaking changes between Java library versions, similar to tools like [japicmp](https://github.com/siom79/japicmp/) or [Revapi](https://github.com/revapi/revapi/).
Whether you're a library maintainer or upgrading dependencies in your projects, Roseau helps ensure backward compatibility across versions.
Roseau analyzes both [JAR files and source code](guides/compare.md), is [highly configurable](reference/cli.md), generates [reports](guides/reports.md) in HTML, JSON, and Markdown, and includes a [dedicated Maven plug-in](guides/maven-plugin.md).

[Get the latest release :material-download:](https://github.com/alien-tools/roseau/releases/latest){ .md-button .md-button--primary }
[View on GitHub :material-github:](https://github.com/alien-tools/roseau){ .md-button }

## Quick Example

When invoked, Roseau extracts the [API surface](api-surface.md) of each version and diffs them to identify breaking changes following a set of well-defined [detection rules](breaking-change-kinds.md).
Library versions can be passed as local JAR files or source directories, or can be fetched remotely from Maven:

```bash
$ roseau --diff --v1 library-1.0.0.jar --v2 library-2.0.0.jar
Breaking changes found: 3 (2 binary-breaking, 2 source-breaking)
✗ com.pkg.A TYPE_REMOVED
  ✗ binary-breaking ✗ source-breaking
  → com/pkg/A.java:4
⚠ com.pkg.B.f FIELD_NOW_STATIC
  ✗ binary-breaking ✓ source-compatible
  → com/pkg/B.java:18
★ com.pkg.C TYPE_NEW_ABSTRACT_METHOD [toOverride()]
  ✓ binary-compatible ✗ source-breaking
  → com/pkg/C.java:210
$ roseau --diff --v1 com.example:lib:1.0.0 --v2 /path/to/v2/src/main/java
[...]
```

An example HTML report is available [here](example-report.html).
