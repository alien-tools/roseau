# Configuration File

Use this page when running Roseau with a `roseau.yaml` file is simpler than passing many CLI options.

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

## Next

- [Check Breaking Changes](compare.md)
- [Report Formats](reports.md)
- [CLI Options](../reference/cli.md)
