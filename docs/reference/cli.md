# CLI Options

This page covers the `--diff` workflow flags exposed by the CLI.

## Modes

!!! note
    `--report=FORMAT=PATH` is repeatable.
    `--binary-only` and `--source-only` cannot be combined.
    Classpath values use the operating-system path separator: `:` on Unix, `;` on Windows.

| Option | Meaning |
| --- | --- |
| `--diff` | Compute breaking changes between versions `--v1` and `--v2` |

## Inputs

| Option | Meaning |
| --- | --- |
| `--v1=<path>` | Path to the first version of the library; either a source directory or a JAR |
| `--v2=<path>` | Path to the second version of the library; either a source directory or a JAR |

## Dependencies

| Option | Meaning |
| --- | --- |
| `--classpath=<path>[,<path>...]` | Shared classpath for `--v1` and `--v2` |
| `--pom=<path>` | Shared `pom.xml` for `--v1` and `--v2` |
| `--v1-classpath=<path>[,<path>...]` | Classpath only for `--v1` |
| `--v2-classpath=<path>[,<path>...]` | Classpath only for `--v2` |
| `--v1-pom=<path>` | `pom.xml` only for `--v1` |
| `--v2-pom=<path>` | `pom.xml` only for `--v2` |

## Filtering

| Option | Meaning |
| --- | --- |
| `--binary-only` | Only report binary-breaking changes |
| `--source-only` | Only report source-breaking changes |
| `--ignored=<path>` | Ignore breaking changes listed in a CSV file |

## Reports

| Option | Meaning |
| --- | --- |
| `--report=<format=path>` | Write a report file in `CLI`, `CSV`, `HTML`, `JSON`, or `MD` format |

## Config and Output

| Option | Meaning |
| --- | --- |
| `--config=<path>` | Load options from `roseau.yaml` |
| `--fail-on-bc` | Return exit code `1` when breaking changes are found |
| `--plain` | Disable ANSI colors |
| `-v`, `--verbose` | Increase verbosity |
