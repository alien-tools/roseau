# Use in CI

Roseau is meant to be easily integrated into build and CI pipelines to spot breaking changes as early as possible. A typical example is as follows:

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar --fail-on-bc --plain \
  --report JSON=reports/breaking-changes.json
```

`--fail-on-bc` makes the process fail when breaking changes are found. `--plain` removes ANSI control codes from the log output. The `--report` option generates a machine-readable JSON report.

## Exit Codes

| Code | Meaning |
| --- | --- |
| `0` | the check completed and no breaking changes were found |
| `1` | `--fail-on-bc` was set and breaking changes were found |
| `2` | the command failed before producing a result |
