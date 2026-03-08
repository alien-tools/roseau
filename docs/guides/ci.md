# Use in CI

Use this page to make Roseau fail a build when breaking changes are detected.

## Basic Command

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

This returns:

- `0`: the command succeeded and no breaking changes were found
- `1`: `--fail-on-bc` was set and breaking changes were found
- `2`: an error occurred

## Write an Artifact

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain \
  --report reports/breaking-changes.json \
  --format JSON
```

Typical CI choices:

- use `--plain` for log output without ANSI escapes
- use `--report` with `CSV` or `JSON` when you want to archive results
- use `--ignored` when some breaking changes are already accepted

## Next

- [Check Breaking Changes](compare.md)
- [Ignore Accepted Breaking Changes](ignored.md)
- [Report Formats](reports.md)
