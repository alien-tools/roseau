# Use in CI

This workflow turns Roseau into a build gate.

## Basic Gate

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain
```

## Exit Codes

| Code | Meaning |
| --- | --- |
| `0` | the check completed and no breaking changes were found |
| `1` | `--fail-on-bc` was set and breaking changes were found |
| `2` | the command failed before producing a result |

## Store an Artifact

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --fail-on-bc \
  --plain \
  --report=JSON=reports/breaking-changes.json
```

Typical CI choices:

- `--plain` for log output without ANSI escapes
- `--report=CSV=...` or `--report=JSON=...` for archived artifacts
- `--ignored` when accepted changes already exist in a baseline CSV
