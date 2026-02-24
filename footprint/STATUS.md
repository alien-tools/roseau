# Footprint Generator Status

## Scope (current)
The `footprint` module generates one deterministic Java client (`Footprint.java`) from an API source tree, with the intent that:
- it compiles, links, and runs on V1
- it fails to compile and/or link on V2 when the API introduces source/binary breaking changes

## Coverage Against `BreakingChangeKind`

### Covered in current generator
- `TYPE_REMOVED`
- `TYPE_NOW_PROTECTED` (for directly accessible exported types)
- `TYPE_KIND_CHANGED`
- `TYPE_SUPERTYPE_REMOVED` (inheritance + explicit upcast/cast probes)
- `TYPE_NEW_ABSTRACT_METHOD`
- `CLASS_NOW_ABSTRACT`
- `CLASS_NOW_FINAL`
- `CLASS_NOW_CHECKED_EXCEPTION` (uncaught throw probes on unchecked throwable types)
- `CLASS_NOW_STATIC`
- `CLASS_NO_LONGER_STATIC`
- `ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT`
- `ANNOTATION_NO_LONGER_REPEATABLE`
- `ANNOTATION_METHOD_NO_LONGER_DEFAULT`
- `FIELD_REMOVED`
- `FIELD_NOW_PROTECTED`
- `FIELD_TYPE_CHANGED`
- `FIELD_NOW_FINAL`
- `FIELD_NOW_STATIC`
- `FIELD_NO_LONGER_STATIC`
- `METHOD_REMOVED`
- `METHOD_NOW_PROTECTED`
- `METHOD_RETURN_TYPE_CHANGED`
- `METHOD_NOW_ABSTRACT`
- `METHOD_NOW_FINAL`
- `METHOD_NOW_STATIC`
- `METHOD_NO_LONGER_STATIC`
- `METHOD_NOW_THROWS_CHECKED_EXCEPTION`
- `METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION`
- `METHOD_PARAMETER_GENERICS_CHANGED`
- `CONSTRUCTOR_REMOVED`
- `CONSTRUCTOR_NOW_PROTECTED`
- `FORMAL_TYPE_PARAMETER_ADDED`
- `FORMAL_TYPE_PARAMETER_REMOVED`
- `FORMAL_TYPE_PARAMETER_CHANGED`

### Partially covered
- `ANNOTATION_TARGET_REMOVED`
- Coverage is direct for supported emission targets (`TYPE`, `METHOD`, `FIELD`, `PARAMETER`, `CONSTRUCTOR`, `LOCAL_VARIABLE`, `TYPE_USE`, `TYPE_PARAMETER`, `RECORD_COMPONENT`, `PACKAGE`).
- `ANNOTATION_TYPE` and `MODULE` targets are not yet emitted as real declaration-site usages in generated client sources.

## Current Implementation Strengths
- Deterministic emission order (types/members/signatures).
- Broad member exercise: constructor calls/references, method invocations/references, overrides, field read/write.
- Protected-member probing through generated subclasses when legal.
- Generic usage generation with explicit type arguments and intersection-bound witness types.
- Checked-exception-aware invocation wrappers that trigger source incompatibilities for throws-clause mutations.
- Uncaught throw probes for unchecked throwable API types.
- Constructor-independent unchecked throw probes (`throw (Type) null`) so unchecked->checked changes are exercised even without usable constructors.
- Generic signature probes for type and method formal type parameters.
- Annotation application emission across supported targets, including repeated-annotation sites.
- Package-target annotation usage emission via generated `package-info.java` companion.
- Explicit supertype assignability/cast probes.
- Protected nested type probes emitted through generated subclass-access contexts.
- `FootprintGeneratorIT` is executed as integration tests via Maven Failsafe (`*IT`) during `verify`.

## Known Gaps
- `ANNOTATION_TYPE` target usages are currently documented in generated comments, but not emitted as real class-scope annotated annotation declarations.
- `MODULE` annotation targets require modular compilation (`module-info.java` + module path) and are not currently exercised.
- Some APIs still trigger fallback-to-raw/unrepresentable paths for generics, which can reduce sensitivity for some source-only generic changes.

## Recommended Improvements
- Add real class-scope `ANNOTATION_TYPE` usage emission (and companion-source handling if needed).
- Add modular test fixtures and optional `module-info.java` generation path for `MODULE` target annotations.
- Keep a strict generation mode that reports and optionally fails on unrepresentable generic constructs.
- Add per-kind mutation tests that isolate each `BreakingChangeKind` and assert expected failure phase (compile-time vs runtime linkage).
