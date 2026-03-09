# Configuration File

Use this page when running Roseau with a `roseau.yaml` file is simpler than passing many CLI options.

Configuration can also refine what Roseau considers part of the API surface. For the baseline accessibility rules, see [What Counts as API](api-surface.md).

## Load a Config File

Load a config file with:

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --config roseau.yaml
```

## Example

Practical example:

```yaml
common:
  classpath:
    pom: /path/to/pom.xml
    jars:
      - /path/to/dependency.jar
  excludes:
    names:
      - com\.example\.internal\..*
    annotations:
      - name: com.google.common.annotations.Beta
      - name: org.apiguardian.api.API
        args:
          status: org.apiguardian.api.API$Status.INTERNAL

diff:
  ignore: /path/to/ignored.csv
  binaryOnly: false
  sourceOnly: false

reports:
  - file: reports/breaking-changes.csv
    format: CSV
  - file: reports/breaking-changes.html
    format: HTML
```

What the main sections do:

- `common`: shared settings for both versions
- `v1` and `v2`: per-version inputs and overrides
- `diff`: diff filters such as `ignore`, `binaryOnly`, and `sourceOnly`
- `reports`: output files to generate

CLI options override config file values.

## Exclude Declarations from the API Surface

Use `excludes` when some declarations are technically accessible but should not be treated as supported API.

Common examples:

- package or symbol naming conventions such as `com\.example\.internal\..*`
- stability annotations such as `com.google.common.annotations.Beta`
- status annotations such as `org.apiguardian.api.API` with `status = INTERNAL`

Roseau applies these exclusions before diffing. If a type is excluded, its nested declarations and members are excluded as well.

## Next

- [What Counts as API](api-surface.md)
- [Check Breaking Changes](compare.md)
- [Report Formats](reports.md)
- [CLI Options](../reference/cli.md)
