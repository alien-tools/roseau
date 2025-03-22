# Build reachability metadata from tests

```
$ mvn -Pnative test native:metadata-copy
$ cp -r src/test/resources/META-INF src/main/resources/META-INF
```

# Build image

```
$ mvn -Pnative package
```
