# Configuration File

`roseau.yaml` keeps repeated diff settings in one file.

Configuration can also refine what Roseau considers part of the API surface. For the baseline accessibility rules, see [What Counts as API](api-surface.md).

## Load a Config File

```bash
java -jar cli/target/roseau-cli-<version>-jar-with-dependencies.jar \
  --diff \
  --config roseau.yaml \
  --v1 path/to/v1.jar \
  --v2 path/to/v2.jar
```

## Example

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

v1:
  location: /path/to/library-1.0.0.jar

v2:
  location: /path/to/library-2.0.0.jar

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

## Sections

| Section | Purpose |
| --- | --- |
| `common` | shared classpath and exclusion rules |
| `v1` | input path and overrides for the baseline version |
| `v2` | input path and overrides for the current version |
| `diff` | filtering options such as `ignore`, `binaryOnly`, and `sourceOnly` |
| `reports` | report files to generate |

CLI options override config file values.

## Exclude Declarations from the API Surface

Use `excludes` when some declarations are technically accessible but should not be treated as supported API.

Common examples:

- package or symbol naming conventions such as `com\.example\.internal\..*`
- stability annotations such as `com.google.common.annotations.Beta`
- status annotations such as `org.apiguardian.api.API` with `status = INTERNAL`

Roseau applies these exclusions before diffing. If a type is excluded, its nested declarations and members are excluded as well.
