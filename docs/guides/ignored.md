# Ignore Accepted Breaking Changes

Accepted changes can be recorded once and filtered out in later runs.

## 1. Generate a Baseline CSV

Generate a CSV report from the current diff:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report=CSV=reports/accepted.csv
```

## 2. Reuse the Baseline

Reuse that file with `--ignored`:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --ignored reports/accepted.csv \
  --plain
```

Minimal accepted CSV format:

```csv
type;symbol;kind
pkg.T;pkg.T.m();METHOD_REMOVED
```

The full CSV report format also works as an ignored file.
