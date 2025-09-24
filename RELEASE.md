## Release checklist

### Remove -SNAPSHOT qualifier and commit
```bash
$ mvn versions:set -DnewVersion=<x.y.z>
$ mvn versions:commit
```

### Confirm we're all good

1. `mvn clean verify`
2. Check Javadoc and README

### Commit, tag, and push
```bash
$ git add <...>
$ git commit -m "Release v<x.y.z>"
$ git tag v<x.y.z>
$ git push
$ git push origin v<x.y.z>
```

### Publish to Sonatype Central
[.github/workflows/release.yml](.github/workflows/release.yml) takes care of that with the appropriate credentials.

  1. [Draft a release](https://github.com/alien-tools/roseau/releases/new)
     - Document changes
  2. Publish the release and wait for `release.yml` to finish
  3. For whatever reason, `release.yml` doesn't trigger if the release is created with attachments.
     Publish first, then add them:
     - Attach `v<x.y.z>-v<x.y.z>-breaking-changes.csv`
     - Attach `roseau-cli-<x.y.z>.jar`
  4. Manually publish the release on https://central.sonatype.com/publishing 

### Prepare next development iteration

```
$ mvn versions:set -DnewVersion=<x.y.z>-SNAPSHOT
$ mvn versions:commit
$ git add <...>
$ git commit -m "Prepare next iteration v<x.y.z>"
$ git push
```
