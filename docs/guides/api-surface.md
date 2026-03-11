# What Counts as API

Roseau does not compare every declaration it can parse. It compares the part of a library that downstream code can use as API through normal Java language and module rules.

In practice, Roseau starts from the Java Language Specification rules for accessibility, then applies Roseau-specific filtering so the compared surface matches what clients can realistically depend on.

## At a Glance

Assume Roseau is configured to exclude symbols under `com.example.internal.*`, symbols annotated `@Beta`, and symbols annotated `@API(status = INTERNAL)`.

**`module-info.java`**

```java
module com.example.lib {
	exports com.example.api;      // API package
	exports com.example.internal; // Still not API if excluded by Roseau config
	// com.example.impl is not exported, so it is not part of the API
}
```

**Representative declarations**

```java
package com.example.api;

import com.google.common.annotations.Beta;
import org.apiguardian.api.API;

public class PublicType { // API
	public void stableMethod() {} // API

	protected void hook() {} // API: protected member on a non-effectively-final type

	protected static class ExtensionPoint { // API: protected nested type that clients can extend
		protected ExtensionPoint() {}
	}

	protected static final class ClosedExtensionPoint { // Not API: effectively final
		protected ClosedExtensionPoint() {}
	}
}

final class PackagePrivateHelper { // Not API: package-private top-level type
}

@Beta
public class ExperimentalType { // Not API: excluded by annotation-based configuration
}

@API(status = API.Status.INTERNAL)
public class InternalStatusType { // Not API: excluded by annotation-based configuration
}
```

```java
package com.example.internal;

public class InternalPackageType { // Not API: excluded by package-name configuration
	public void stillExcluded() {} // Not API
}
```

```java
package com.example.impl;

public class ImplType { // Not API in a named module: package is not exported
	public void hiddenFromClients() {} // Not API
}
```

```java
package com.example.api;

public final class ClosedType { // API
	protected void cannotBeUsedAsExtensionHook() {} // Not API: containing type is effectively final
}
```

## Baseline Rules

Roseau treats a declaration as API only if client code can access it without reflection or other special tricks.

- Visibility matters: public declarations are candidates; private declarations are not.
- Module boundaries matter: in a named module, a package must be exported with an unqualified `exports` directive to contribute to the API surface.
- Without a module declaration, Roseau falls back to regular Java visibility rules.
- Package-private declarations are not considered API, even if a client could technically reach them by re-opening a package or using other pathological setups.

That last point is intentional. Roseau models API that regular clients can consume directly, not declarations that are only reachable through reflective access, module patching, or package re-opening.

## Types

Public top-level types are part of the API surface when their package is part of the library's exported surface.

Protected nested types are more subtle. Roseau only treats a protected type as API when client code can actually extend it. In other words, a protected type counts as API only when it is not effectively final.

Roseau treats a type as effectively final when clients cannot reasonably derive from it, for example because it is:

- `final`
- `sealed` rather than `non-sealed`
- a class with no constructor accessible to subclasses

If a protected nested type cannot be extended, Roseau does not treat it as API.

## Members

Public fields, methods, and constructors are API only when they belong to an API type.

Protected members are part of the API surface only when both of these conditions hold:

- the member itself is accessible to clients
- the containing type is not effectively final

This matches how protected members are used in practice: they matter only when client code can subclass the type and access or override them through normal Java rules.

## User-Configured Exclusions

After applying the baseline Java visibility and module rules, Roseau applies the exclusions configured by the user.

This removes declarations that are technically accessible but should not be treated as supported API, such as:

- stability annotations like `@Beta`
- API status annotations like `@API(status = INTERNAL)`
- package or symbol naming conventions like `*.internal.*`

These exclusions are part of the API definition, not just post-processing on the final report. If a symbol is excluded, Roseau removes it from the API surface before diffing.

Use the [Configuration File](config.md) guide to configure name-based and annotation-based exclusions.

## Practical Reading

When Roseau says a type or member is "exported", read that as:

- accessible through normal Java use
- visible across module boundaries when modules are present
- not filtered out by Roseau's exclusion rules

That is the surface Roseau compares when it reports breaking changes.
