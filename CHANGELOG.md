# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- Added a `roseau-maven-plugin` module with a `check` goal for comparing the current artifact against a baseline JAR or Maven coordinates during `verify`, generating reports, exporting API snapshots, and optionally failing the build on incompatibilities.

### Changed
- **Breaking:** Replaced the CLI `--report <path>` and `--format <format>` pair with a repeatable `--report FORMAT=PATH` option, allowing multiple reports to be written in a single run.

### Fixed
- Refined binary/source compatibility classification by splitting existing kinds into source-/binary-specific kinds.
