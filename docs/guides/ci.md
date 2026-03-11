# Use in CI

This workflow turns Roseau into a build gate.

## Minimal Gate

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

`--fail-on-bc` makes the process fail when breaking changes are found. `--plain` removes ANSI control codes from the log output.

## Exit Codes

| Code | Meaning |
| --- | --- |
| `0` | the check completed and no breaking changes were found |
| `1` | `--fail-on-bc` was set and breaking changes were found |
| `2` | the command failed before producing a result |

## Typical CI Step

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 artifacts/library-previous.jar \
  --v2 target/library-current.jar \
  --fail-on-bc \
  --plain \
  --report=JSON=reports/breaking-changes.json
```

## Common CI Choices

| Need | Option |
| --- | --- |
| plain logs | `--plain` |
| CI failure on incompatibility | `--fail-on-bc` |
| archived machine-readable artifact | `--report=JSON=...` |
| accepted-change baseline | `--ignored accepted.csv` |
| narrower checks | `--binary-only` or `--source-only` |
