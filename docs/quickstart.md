# Quickstart

## 1. Get Roseau

=== "Standalone (no JDK required)"

    Download the archive for your platform from the [latest release](https://github.com/alien-tools/roseau/releases/latest), unzip it, and add the `bin/` directory to your `PATH`.

    ```bash
    unzip roseau-<version>-linux-x86_64.zip
    export PATH="$PWD/roseau-<version>/bin:$PATH"
    ```

    Verify the installation:

    ```bash
    roseau --version
    ```

=== "JAR (requires Java 25)"

    Download `roseau-<version>.jar` from the [latest release](https://github.com/alien-tools/roseau/releases/latest).

    ```bash
    java -jar roseau-<version>.jar --version
    ```

    The examples on this page use the `roseau` command. Replace it with `java -jar roseau-<version>.jar` when using the JAR directly.

=== "Build from source"

    Roseau can be cloned and built from source.

    ```bash
    git clone https://github.com/alien-tools/roseau.git
    cd roseau
    ./mvnw --batch-mode package
    alias roseau='java -jar $PWD/cli/target/roseau-<version>.jar'
    ```

## 2. Run a First Diff

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar
```

`--v1` and `--v2` can each be a JAR file or a source directory (typically similar to `src/main/java`).

Roseau compares the library API surface, not every declaration in the input. For the exact rules, see [what Roseau considers API](api-surface.md).

## 3. Read the Result

Typical CLI output looks like this:

```text
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
```

Each entry begins with a symbol denoting the nature of the change (✗ removal, ⚠ change, ★ addition), followed by the affected symbol (e.g., a type, a method, a field) and the [kind of breaking change](breaking-change-kinds.md) that was identified. The second line shows binary and source compatibility. The third line gives the location of the impacted symbol in the first version.

## 4. Write Report Files

Roseau can generate reports in [various formats](guides/reports.md).

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar \
  --report CSV=report.csv --report HTML=report.html
```

`--report=FORMAT=PATH` can be repeated. Available formats: `CLI`, `CSV`, `HTML`, `JSON`, `MD`.

## 5. Use Roseau in CI or to gate a build

By default, Roseau does not exit with an error code when breaking changes are identified. The `--fail-on-bc` flag can be used for that purpose. Together with `--plain`, it can be used to gate a build in CI.

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar --fail-on-bc --plain
```

`--fail-on-bc` exits with code `1` when breaking changes are found. `--plain` strips ANSI colors for log files and CI output.

## 6. Specify API exclusions

Use `excludes` when some symbols are technically accessible by client code but should not be treated as supported API. Roseau does not report any breaking change affecting excluded symbols. If a type is excluded, its nested declarations and members are excluded as well.

Common examples include package or symbol naming conventions such as `com\.example\.internal\..*` and stability annotations such as `com.google.common.annotations.Beta` or `org.apiguardian.api.API(status = INTERNAL)`.
These can be configured in the [YAML configuration file](guides/config.md):

```yaml
common:
  excludes:
    names:
      - com\.example\.internal\..*
    annotations:
      - name: com.google.common.annotations.Beta
      - name: org.apiguardian.api.API
        args: { status: org.apiguardian.api.API$Status.INTERNAL }
```

## Next Steps

| Question | Page |
| --- | --- |
| Which input pair fits the workflow? | [Compare Two Versions](guides/compare.md) |
| Which declarations are compared? | [API Surface](api-surface.md) |
| Which report format to use? | [Report Formats](guides/reports.md) |
| What breaking change kinds can appear? | [Breaking Change Kinds](breaking-change-kinds.md) |
