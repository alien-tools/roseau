# CLI options

This page documents Roseau's CLI options.

!!! note
    `--report=FORMAT=PATH` is repeatable.
    `--binary-only` and `--source-only` cannot be combined.
    Classpath values use the operating-system path separator: `:` on Unix, `;` on Windows.

## Modes

Exactly one mode must be specified.

| Option | Meaning                                                                                                                      |
| --- |------------------------------------------------------------------------------------------------------------------------------|
| `--diff` | Compute breaking changes between versions `--v1` and `--v2`                                                                  |
| `--api` | Extract and serialize the API model of `--v1` as JSON and prints it to stdout; `--api-json=api.json` serializes it to a file |

## Inputs

| Option | Meaning |
| --- | --- |
| `--v1=<path>` | Path to the first version of the library; either a source directory or a JAR |
| `--v2=<path>` | Path to the second version of the library; either a source directory or a JAR (`--diff` mode only) |
| `--api-json=<path>` | Output path for the JSON API model (`--api` mode only) |

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
| `--report=<format=path>` | Write a report file in `CLI`, `CSV`, `HTML`, `JSON`, or `MD` format; repeatable |

## Config and Output

| Option | Meaning |
| --- | --- |
| `--config=<path>` | Load options from a `roseau.yaml` file; CLI options take precedence |
| `--fail-on-bc` | Return exit code `1` when breaking changes are found |
| `--plain` | Disable ANSI colors, output plain text |
| `-v`, `--verbose` | Increase verbosity: `-v` for verbose output, `-vv` for debug output |

## Exit Codes

| Code | Meaning |
| --- | --- |
| `0` | The check completed and no breaking changes were found (or `--fail-on-bc` was not set) |
| `1` | `--fail-on-bc` was set and breaking changes were found |
| `2` | The command failed before producing a result |
