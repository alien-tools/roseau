# Footprint Generator Status

## Scope (current)
The `footprint` module generates a single `Footprint.java` from a source tree API and targets:
- deterministic generation order
- successful compile, link, and run on V1
- compilation or linkage failure on V2 for breaking changes

## What Is Covered
- Exported accessible types: class, interface, enum, annotation, record, nested public types.
- Type usages:
  - raw type references
  - type tokens (`.class`)
  - parameterized references (including bounded generics where representable)
- Class usages:
  - direct constructor invocations (when legal)
  - constructor references (`Type::new`) (when legal)
  - subclass generation and instantiation (when legal)
  - abstract-class implementation via generated subclass and anonymous subclass (when legal)
- Interface usages:
  - extension (`interface X extends I`)
  - anonymous implementation (`new I() { ... }`)
- Method usages:
  - invocation (instance/static)
  - varargs call forms (expanded and array)
  - explicit type-argument invocations when representable
  - overriding in generated subclasses
  - method references (bound form, e.g. `expr::m`)
- Field usages:
  - read/write for mutable fields
  - static and instance field access
  - protected field access through generated subclasses (when legal)
- Exception handling:
  - checked-exception-aware try/catch around executable uses
  - `throws` propagation in generated override/implementation signatures
  - explicit throwable-type exercise (`throws`, `throw`, `catch`) for exception classes
- Accessibility policy:
  - package-private top-level API symbols are ignored
  - inaccessible/unrepresentable symbols produce explanatory comments
- Determinism:
  - stable sorting of types/members/signatures

## Test Coverage
`FootprintGeneratorIT` validates:
- V1: generated footprint compiles, links, and runs successfully
- V2 source break: V1 footprint fails compilation against source-breaking API changes
- V2 binary break: V1-compiled footprint fails at runtime (linkage error) against binary-breaking API changes
- Generated source contains core usage forms (`.class`, `::`, `::new`, inheritance, checked exception handling, bounded generic references)

## Known Gaps (not fully covered yet)
- Protected nested types are not exercised as types/members when only accessible through subclass context.
- Protected members of non-static inner classes are not exercised (no generated `outer.super(...)` subclass pattern yet).
- Unbound instance method references (`Type::instanceMethod`) are not emitted; only bound references (`expr::instanceMethod`) are generated.
