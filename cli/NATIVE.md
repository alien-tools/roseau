# Build reachability metadata from tests

```
$ mvn -Pnative test native:metadata-copy
$ cp -r src/test/resources/META-INF src/main/resources/META-INF
```

# Build image

```
$ native-image --no-fallback -H:IncludeResources="org/eclipse/jdt/internal/compiler/batch/messages.properties" -H:IncludeLocales=en -jar cli/target/roseau-cli-0.3.0-SNAPSHOT-jar-with-dependencies.jar
$ mvn -Pnative package
```
