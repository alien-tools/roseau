# Maven Plug-in

Use this page to check breaking changes automatically during your Maven build.

The Roseau Maven plug-in compares the current artifact against a baseline during the `verify` phase and can fail the build when breaking changes are detected.

## Minimal Setup

Add the plug-in to your `pom.xml` and provide a baseline to compare against:

```xml
<plugin>
  <groupId>io.github.alien-tools</groupId>
  <artifactId>roseau-maven-plugin</artifactId>
  <version>0.6.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <baselineJar>${project.basedir}/old.jar</baselineJar>
  </configuration>
</plugin>
```

Then run:

```bash
mvn verify
```

The plug-in runs during the `verify` phase. It compares the JAR produced by the current build against the baseline and prints any breaking changes to the Maven log.

## Choose a Baseline

You can specify a baseline as a local JAR file or as Maven coordinates that are resolved from your repositories.

**Local JAR file**

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
</configuration>
```

**Maven coordinates**

```xml
<configuration>
  <baselineVersion>
    <groupId>com.example</groupId>
    <artifactId>my-library</artifactId>
    <version>1.0.0</version>
  </baselineVersion>
</configuration>
```

When both are provided, `baselineVersion` takes precedence.

## Fail the Build on Breaking Changes

By default, the plug-in reports breaking changes but does not fail the build. Use one of the following options to gate your build:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <!-- Fail on any breaking change -->
  <failOnIncompatibility>true</failOnIncompatibility>
</configuration>
```

For finer control:

| Option | Fails when |
| --- | --- |
| `failOnIncompatibility` | any breaking change is found |
| `failOnBinaryIncompatibility` | a binary-breaking change is found |
| `failOnSourceIncompatibility` | a source-breaking change is found |

## Filter by Compatibility Kind

Report only binary-breaking or only source-breaking changes:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <binaryOnly>true</binaryOnly>
</configuration>
```

Or:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <sourceOnly>true</sourceOnly>
</configuration>
```

## Generate Reports

Write reports to files. Relative paths are resolved against `reportDirectory`, which defaults to `${project.build.directory}/roseau`.

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <reports>
    <report>
      <file>report.csv</file>
      <format>CSV</format>
    </report>
    <report>
      <file>report.html</file>
      <format>HTML</format>
    </report>
  </reports>
</configuration>
```

Available formats: `CSV`, `HTML`, `JSON`, `MD`.

## Provide a Classpath

The plug-in automatically includes the current project's compile-scope dependencies in the classpath.

To add extra entries shared by both baseline and current artifact:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <classpath>
    <path>/path/to/extra-dependency.jar</path>
  </classpath>
</configuration>
```

To add entries only for the baseline:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <baselineClasspath>
    <path>/path/to/old-dependency.jar</path>
  </baselineClasspath>
</configuration>
```

You can also use a POM file to derive classpath entries:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <classpathPom>/path/to/pom.xml</classpathPom>
</configuration>
```

## Export API Models

Export the API models as JSON for archival or further analysis:

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <exportBaselineApi>${project.build.directory}/roseau/baseline-api.json</exportBaselineApi>
  <exportCurrentApi>${project.build.directory}/roseau/current-api.json</exportCurrentApi>
</configuration>
```

## Use a Configuration File

Supply a `roseau.yaml` file. Maven parameters take precedence over YAML options.

```xml
<configuration>
  <baselineJar>${project.basedir}/old.jar</baselineJar>
  <configFile>roseau.yaml</configFile>
</configuration>
```

See [Configuration File](config.md) for the YAML format.

## Skip Execution

Skip the plug-in from the command line:

```bash
mvn verify -Droseau.skip=true
```

Or in the POM:

```xml
<configuration>
  <skip>true</skip>
</configuration>
```

!!! note
    The plug-in automatically skips projects with `pom` packaging.

## Configuration Reference

All parameters can be set as Maven properties on the command line using `-D`.

| Parameter | Property | Default | Description |
| --- | --- | --- | --- |
| `baselineVersion` | `roseau.baselineVersion` | — | Baseline Maven coordinates (groupId, artifactId, version) |
| `baselineJar` | `roseau.baselineJar` | — | Path to a baseline JAR file |
| `failOnIncompatibility` | `roseau.failOnIncompatibility` | `false` | Fail on any breaking change |
| `failOnBinaryIncompatibility` | `roseau.failOnBinaryIncompatibility` | `false` | Fail on binary-breaking changes |
| `failOnSourceIncompatibility` | `roseau.failOnSourceIncompatibility` | `false` | Fail on source-breaking changes |
| `binaryOnly` | `roseau.binaryOnly` | — | Report only binary-breaking changes |
| `sourceOnly` | `roseau.sourceOnly` | — | Report only source-breaking changes |
| `reports` | — | — | List of report files to generate |
| `reportDirectory` | `roseau.reportDirectory` | `${project.build.directory}/roseau` | Output directory for relative report paths |
| `classpath` | — | — | Extra classpath entries for both versions |
| `classpathPom` | — | — | POM for deriving shared classpath |
| `baselineClasspath` | — | — | Extra classpath entries for the baseline |
| `baselineClasspathPom` | — | — | POM for deriving baseline classpath |
| `exportBaselineApi` | `roseau.exportBaselineApi` | — | Export baseline API as JSON |
| `exportCurrentApi` | `roseau.exportCurrentApi` | — | Export current API as JSON |
| `configFile` | `roseau.configFile` | — | Path to a `roseau.yaml` file |
| `verbosity` | `roseau.verbosity` | — | Logging level: QUIET, NORMAL, VERBOSE, DEBUG |
| `skip` | `roseau.skip` | `false` | Skip execution |

## Next

- [Use in CI](ci.md)
- [Report Formats](reports.md)
- [Configuration File](config.md)
