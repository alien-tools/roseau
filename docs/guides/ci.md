# Use in CI

Use this page to make Roseau fail a build when breaking changes are detected.

Basic CI-friendly command:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

This returns:

- `0` when the command succeeds and no breaking changes are found
- `1` when `--fail-on-bc` is set and breaking changes are found
- `2` on errors

Write a machine-readable artifact:

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

- Use `--plain` for log output without ANSI escapes.
- Use `--report` with `CSV` or `JSON` when you want to archive results.
- Use `--ignored` when some breaking changes are already accepted.
