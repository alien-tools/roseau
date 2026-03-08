# Reports

Use this page to write Roseau results to files.

## Single Report from the CLI

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/breaking-changes.json \
  --format JSON
```

## Formats

- `CLI`: terminal-style text
- `CSV`: easy to review, diff, and reuse with `--ignored`
- `HTML`: readable report for people
- `JSON`: structured output for automation
- `MD`: Markdown report

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

## Next

- [Check Breaking Changes](compare.md)
- [Use in CI](ci.md)
- [Ignore Accepted Breaking Changes](ignored.md)
