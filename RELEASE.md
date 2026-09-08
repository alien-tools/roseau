# Releasing Roseau

Roseau uses JReleaser and GitHub Actions to publish Maven artifacts to Central and a GitHub release with the CLI JAR, five standalone platform archives, signatures, and checksums. Release notes are generated from conventional commits.

- [`build-main.yml`](.github/workflows/build-main.yml) publishes snapshots and updates `early-access` when `main` has a `-SNAPSHOT` version. Stable versions are skipped.
- [`release.yml`](.github/workflows/release.yml) supports a manual rehearsal and publishes when a `v*` tag matches the Maven version. This workflow must be merged into `main` before manual runs are available.

## 1. Prepare

Start from an up-to-date `main`, create a release branch, and replace `X.Y.Z` with the version to release:

```bash
./mvnw --batch-mode versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
```

Update the release versions in `README.md`, `docs/`, and `CITATION.cff`. Keep historical examples and benchmark versions unchanged unless their results have been regenerated.

With JDK 25, verify locally:

```bash
./mvnw --batch-mode -Pinclude-smoke clean verify
git diff --check
```

Commit the changes, push the release branch, and open a pull request.

## 2. Rehearse

Manually run **Publish release from tag** in GitHub Actions against the release branch. It tests on Linux, macOS, and Windows, stages the Maven artifacts, builds all five standalone archives, and validates the JReleaser configuration and generated notes without publishing.

Wait for all jobs to pass, then merge. Run the rehearsal against `main` and confirm its commit is the one you will tag. The generated notes are available in the `release-dry-run-notes` workflow artifact.

## 3. Publish

Check out the tested commit, confirm the version has no `-SNAPSHOT` suffix and has not already been released, then push an annotated tag:

```bash
git tag -a vX.Y.Z -m "Roseau X.Y.Z"
git push origin refs/tags/vX.Y.Z
```

Use `git tag -s` instead if signing tags. Wait for **Publish release from tag** to finish. It uses the existing repository secrets for Central and GPG signing.

Before announcing, check that the four Maven artifacts (`roseau-parent`, `roseau-core`, `roseau-cli`, and `roseau-maven-plugin`) resolve from Central, the GitHub release includes all assets, and the downloaded CLI and standalone launcher report the correct version. Verify the downloaded checksums and signatures.

Central releases cannot be overwritten. If publication fails, check whether Central accepted the version before retrying; never move a published tag or redeploy an accepted version.

## 4. Resume development

On a new branch, replace `NEXT` with the next development version:

```bash
./mvnw --batch-mode versions:set -DnewVersion=NEXT-SNAPSHOT -DgenerateBackupPoms=false
```

Commit and merge through a pull request. Keep public examples and citation metadata on the latest stable version.
