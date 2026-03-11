# Reports

Roseau can write one or more report files during the same diff run.

## CLI Syntax

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report=JSON=reports/breaking-changes.json
```

## Formats

| Format | Typical use |
| --- | --- |
| `CLI` | terminal-style text written to a file |
| `CSV` | review, diffing, and reuse with `--ignored` |
| `HTML` | human-readable report artifacts |
| `JSON` | automation and post-processing |
| `MD` | Markdown reports for release notes or pull requests |

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

```yaml
reports:
  - file: reports/breaking-changes.csv
    format: CSV
  - file: reports/breaking-changes.html
    format: HTML
  - file: reports/breaking-changes.json
    format: JSON
```
