# Java release

Whether a change breaks clients depends on the Java platform those clients run on. Roseau compares both versions of a library on one Java release.

## What the release changes

On Java `N`, Roseau sees compiled JARs as clients compiled with `javac --release N` and run on Java `N` see them:

- **JDK APIs.** JDK types and members are those of Java `N`, read from the `lib/ct.sym` of the JDK running Roseau, as for `javac --release N`.
- **Multi-release JARs.** Roseau reads the entries that Java `N` loads, both in the analyzed JARs and in their classpath dependencies.
- **Modules.** Java 8 has no modules, so below release 9 Roseau ignores module declarations. From release 9 on, module declarations apply unless [`ignoreModule`](api-surface.md) is set.

For example, take a library that targets Java 8 and declares `public List<String> reversed()` in a class extending `ArrayList<String>`. Removing that method breaks Java 8 clients (`NoSuchMethodError`). Java 21 clients are unaffected, because the call resolves to `List.reversed()`, a default method added in Java 21. Roseau reports the removal with `--java-release 8` and not with `--java-release 21`.

## Choosing the release

The release defaults to the release of the JDK running Roseau. Pick the oldest release the library supports to catch the changes that break its oldest clients. For multi-release JARs, also check the releases that select different implementations. The release can be set on the CLI, in the YAML configuration, and in the Maven plug-in.

`N` is a feature release such as `8`, `17`, or `21`. Both versions are always compared on the same release.

Supported releases range from 8 to the release of the JDK running Roseau, as for `javac --release`.

!!! note
    Source directories can only be checked on the release of the JDK running Roseau for now. To check compatibility on another release, compare compiled JARs instead.
