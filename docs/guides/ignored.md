# Ignore Accepted Breaking Changes

Use this page when some breaking changes are known and should stop failing your workflow.

First generate a CSV report:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar \
  --report reports/accepted.csv \
  --format CSV
```

Then reuse that file with `--ignored`:

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
