## Release process

Roseau uses [JReleaser](https://jreleaser.org/) for release automation. Releases are published to Maven Central and GitHub Releases, including platform-specific `jlink` archives.

### Snapshot releases

Every push to `main` triggers [build-main.yml](.github/workflows/build-main.yml), which:

1. Builds and tests the project
2. Stages Maven artifacts to a local repository
3. Assembles jlink archives for all platforms (Linux x86_64/aarch64, macOS x86_64/aarch64, Windows x86_64)
4. Publishes SNAPSHOT artifacts to Maven Central's snapshot repository
5. Creates/updates a rolling `early-access` prerelease on GitHub with the shaded CLI JAR and `jlink` archives

### Stable releases

#### 1. Remove the -SNAPSHOT qualifier

```bash
mvn versions:set -DnewVersion=<x.y.z>
mvn versions:commit
```

#### 2. Verify the build

```bash
./mvnw --batch-mode verify
```

Check Javadoc and update `CHANGELOG.md` with release notes.

#### 3. Commit, tag, and push

```bash
git add -A
git commit -m "Release v<x.y.z>"
git tag v<x.y.z>
git push
git push origin v<x.y.z>
```

Pushing the `v*` tag triggers [release.yml](.github/workflows/release.yml), which:

1. Builds, tests, and stages Maven artifacts
2. Assembles `jlink` archives for all platforms
3. Publishes release artifacts to Maven Central via JReleaser
4. Creates a GitHub Release with the shaded CLI JAR and `jlink` archives

#### 4. Prepare next development iteration

```bash
mvn versions:set -DnewVersion=<next.version>-SNAPSHOT
mvn versions:commit
git add -A
git commit -m "Prepare next iteration v<next.version>"
git push
```
