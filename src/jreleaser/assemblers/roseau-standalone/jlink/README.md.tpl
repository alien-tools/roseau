# Roseau {{projectVersion}}

This archive contains the standalone Roseau CLI together with a bundled Java runtime.
No separate Java installation is required.

## Usage

- macOS and Linux: `bin/{{distributionExecutable}} --help`
- Windows: `bin\{{distributionExecutable}}.bat --help`

## Examples

- Show the CLI version: `bin/{{distributionExecutable}} --version`
- Compare two libraries: `bin/{{distributionExecutable}} --diff --v1 old.jar --v2 new.jar`

Use the shaded JAR release instead if you need a JVM-based fallback for an unsupported platform.
