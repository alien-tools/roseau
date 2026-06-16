# Ignoring known breaking changes

Breaking changes are sometimes unavoidable or intentional. Roseau can therefore be configured to ignore a list of known breaking changes to avoid reporting them in subsequent analyses. The list of ignored changes is specified in a CSV file that shares the same format as the output of the `--report` option.

## 1. Generate a Baseline CSV

Generate a CSV report from the current diff:

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar \
  --report CSV=reports/accepted.csv
```
The generated CSV file looks as follows and can be reused to ignore the listed changes in future analyses.

```csv
type;symbol;kind
pkg.T;pkg.T.m();EXECUTABLE_REMOVED
```

## 2. Reuse the Baseline

Passing that file to the `--ignored` option will cause Roseau to ignore all changes listed in the CSV file.

```bash
roseau --diff --v1 path/to/v1.jar --v2 path/to/v2.jar \
  --ignored reports/accepted.csv
```
