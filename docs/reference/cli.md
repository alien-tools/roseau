# CLI Options

Use this page when you need the exact meaning of a CLI flag.

## Modes

- `--diff`: compare `--v1` and `--v2`

Roseau also supports `--api` with `--api-json` for exporting the API of `--v1`, but this site focuses on the `--diff` workflow.

## Inputs

- `--v1 <path>`: first library version; can be a JAR or source directory
- `--v2 <path>`: second library version; can be a JAR or source directory

## Dependencies

- `--classpath <paths>`: shared classpath for `--v1` and `--v2`
- `--pom <path>`: shared `pom.xml` for `--v1` and `--v2`
- `--v1-classpath <paths>`: classpath only for `--v1`
- `--v2-classpath <paths>`: classpath only for `--v2`
- `--v1-pom <path>`: `pom.xml` only for `--v1`
- `--v2-pom <path>`: `pom.xml` only for `--v2`

Use the OS path separator in classpath values: `:` on Unix, `;` on Windows.

## Filtering

- `--binary-only`: only report binary-breaking changes
- `--source-only`: only report source-breaking changes
- `--ignored <path>`: ignore breaking changes listed in a CSV file

`--binary-only` and `--source-only` cannot be used together.

## Reports

- `--report <path>`: write the report to a file
- `--format <CLI|CSV|HTML|JSON|MD>`: output format for `--report`

`--report` requires `--format`.

## Config and Output

- `--config <path>`: load options from `roseau.yaml`
- `--fail-on-bc`: return exit code `1` when breaking changes are found
- `--plain`: disable ANSI colors
- `-v`, `--verbose`: increase verbosity

CLI options override config file values.
