# Reports

Roseau can write one or more report files during the same diff run.

## Report Syntax

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report=JSON=reports/breaking-changes.json
```

`--report=FORMAT=PATH` can be repeated to produce several artifacts in one pass.

## Choose a Format

| Format | Best fit |
| --- | --- |
| `CLI` | terminal-style text written to a file |
| `CSV` | review, diffing, and reuse with `--ignored` |
| `HTML` | human-readable report artifacts |
| `JSON` | automation and post-processing |
| `MD` | Markdown reports for release notes or pull requests |

## Recommended Combinations

| Workflow | Recommended reports |
| --- | --- |
| local review | `HTML` or `CLI` |
| CI artifact retention | `JSON` or `CSV` |
| accepted-change baseline | `CSV` |
| lightweight sharing in issue trackers or changelogs | `MD` |

## Multiple Reports from the CLI

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report=CSV=reports/breaking-changes.csv \
  --report=HTML=reports/breaking-changes.html \
  --report=JSON=reports/breaking-changes.json
```

## Multiple Reports from `roseau.yaml`

For repeated workflows, the same report list can live in configuration:

```yaml
reports:
  - file: reports/breaking-changes.csv
    format: CSV
  - file: reports/breaking-changes.html
    format: HTML
  - file: reports/breaking-changes.json
    format: JSON
```
