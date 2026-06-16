# What Roseau considers API

When analyzing a JAR or a source tree, Roseau does not look for breaking changes in every declaration it finds. It first infers the *API surface* of each version by following Java's accessibility rules and any user-defined [exclusion rules](guides/config.md). In practice, Roseau follows the accessibility rules defined in the [Java Language Specification](https://docs.oracle.com/javase/specs/jls/se25/jls25.pdf) (e.g., visibility modifiers, module boundaries) to accurately determine which symbols client code can access.

## Baseline Rules

Roseau treats a symbol as part of the API only if client code can access it without reflection or other tricks.

- **Visibility:** `public` and `protected` declarations are candidates; `private` and package-private declarations are not. A nested type or member must itself be accessible, and its enclosing type must be accessible as well.
- **Module boundaries:** In a named module, a package must appear in an unqualified `exports` directive to contribute to the API surface. Qualified exports (`exports ... to ...`) are not considered public API.
- **No module declaration:** If the library has no `module-info`, Roseau treats all packages as accessible.

!!! note "Package-private declarations"
    Package-private declarations are not considered API, even if a client could technically access them by placing code in the same package or using other non-standard means.

### Types

Public top-level and nested types are part of the API surface when their package is exported (or the library is not modular).

Protected nested types require an additional check. A protected nested type is only accessible to subclasses of its enclosing type, so Roseau treats a protected nested type as API only when its enclosing type is **not effectively final**.

A type is *effectively final* when client code cannot subclass it:

- the type is declared `final`
- the type is part of a `sealed` hierarchy (but is not `non-sealed`)
- the type is an `enum` or a `record`, both of which are implicitly final
- the type is a class whose only declared constructors are `private` (i.e., it has no `public` or `protected` subclass-accessible constructor)

These conditions are evaluated at every level of nesting. For a deeply nested type to be part of the API, each type in its enclosing chain must independently satisfy the applicable accessibility conditions.

### Members

Public fields, methods, and constructors are part of the API when they belong to an API type.
Protected members are part of the API only when both conditions hold:

- their containing type is itself part of the API surface
- their containing type is not effectively final (a type that cannot be subclassed makes its protected members unreachable to clients)

### User-Configured Exclusions

After applying the baseline visibility and module rules, Roseau applies any exclusions configured by the user. This removes declarations that are technically accessible but should not be treated as supported API, such as:

- symbols matching a name pattern like `*.internal.*`
- symbols carrying a stability annotation like `@Beta` or `@API(status = INTERNAL)`

Use the [Configuration File](guides/config.md) guide to configure name-based and annotation-based exclusions.

## Example

Assume Roseau is configured to exclude symbols under `com.example.internal.*` and symbols annotated `@Beta` or `@API(status = INTERNAL)`, and the library declares the following module:

```java title="module-info.java"
module com.example.lib {
	exports com.example.api;      // API package
	exports com.example.internal; // Still not API: excluded by name
	// com.example.impl is not exported, so it is not API
}
```

Roseau classifies the following declarations as API or not:

```java
package com.example.api;

import com.google.common.annotations.Beta;
import org.apiguardian.api.API;

public class C1 { // API
	private String f1; // Not API: private
	public void m1() {} // API
	protected void m2() {} // API: protected member of a non-effectively-final type
	protected static class N {} // API: protected nested type within a non-effectively-final type
}

class C2 {} // Not API: package-private

public final class C3 { // API
	protected String f1; // Not API: protected member of an effectively-final type
	public void m1() {} // API
	protected void m2() {} // Not API: protected member of an effectively-final type
}

@Beta
public class C4 {} // Not API: excluded by annotation

@API(status = API.Status.INTERNAL)
public class C5 {} // Not API: excluded by annotation
```

```java
package com.example.internal;

public class C { // Not API: excluded by name
	public void m() {} // Not API
}
```

```java
package com.example.impl;

public class C { // Not API: package not exported by the module
	public void hiddenFromClients() {} // Not API
}
```
