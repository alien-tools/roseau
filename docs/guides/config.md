# YAML configuration

When configuration options start piling up, Roseau can read a YAML configuration file with the `--config` option.
The configuration can also refine what Roseau considers part of the [API surface](../api-surface.md), for example by excluding certain symbols based on naming conventions or annotations.
For the baseline rules, see [What Roseau considers API](../api-surface.md).

## When a Config File Helps

`roseau.yaml` is useful when one of these patterns appears repeatedly:

- the same classpath or `pom.xml` is supplied on every run
- the same exclusions define the supported API surface
- the same report files are generated in CI or release workflows
- the same ignored-change baseline is reused against a fixed reference version

## Load the File

The configuration file is loaded using the `--config` option. Note that in case of conflicts, CLI options take precedence over values configured in the YAML file.

```bash
roseau --diff --config roseau.yaml --v1 path/to/v1.jar --v2 path/to/v2.jar
```

## Specifying API exclusions

Use `excludes` when some symbols are technically accessible by client code but should not be treated as supported API. Roseau does not report any breaking change affecting excluded symbols. If a type is excluded, its nested declarations and members are excluded as well.

Common examples:

- package or symbol naming conventions such as `com\.example\.internal\..*`
- stability annotations such as `com.google.common.annotations.Beta` or `org.apiguardian.api.API(status = INTERNAL)`

```yaml title="roseau.yaml"
common:
  excludes:
    names:
      - com\.example\.internal\..*
    annotations:
      - name: com.google.common.annotations.Beta
      - name: org.apiguardian.api.API
        args: { status: org.apiguardian.api.API$Status.INTERNAL }
```

## Example configuration

| Section | Meaning |
| --- | --- |
| `common` | shared classpath and exclusion rules |
| `v1` | input path and overrides for the baseline version |
| `v2` | input path and overrides for the current version |
| `diff` | filtering options such as `ignore`, `binaryOnly`, and `sourceOnly` |
| `reports` | report files to generate |

```yaml title="roseau.yaml"
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
        args: { status: org.apiguardian.api.API$Status.INTERNAL }

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
