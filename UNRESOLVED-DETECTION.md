# Detecting lossy JDT extraction so `--fail-on-unresolved` is reliable

Status: agreed design, not implemented. Branch `unresolved-types-resolution-bug` (commit `8be12b0c`) holds the failing
tests that motivated it.

## Summary

When a library is analyzed from sources without its full classpath, JDT silently loses parts of the API whenever a
missing type is written as a **dotted name with type arguments** (`a.b.Missing<String>`, `Outer.Inner<String>`,
`Outer<String>.Inner`, `List<a.b.Missing<String>>`). Members disappear, supertypes and bounds become `Object`, and
records get a canonical constructor that does not exist. Nothing reaches `getUnresolvedTypes()`, so
`--fail-on-unresolved` passes and the report is silently wrong.

We will **not** try to recover the lost declarations (rebuilding members, records and implicit modifiers from the AST
is too complex for its value). Instead, Roseau will **detect every declaration it could not extract faithfully and
report it**: always as a warning, and as a failure under `--fail-on-unresolved`. The goal is that, in practice, a run
that passes `--fail-on-unresolved` has a complete and faithful API model.

## The bug

### Root cause (ecj)

`JdtTypesExtractor` enables binding recovery so that missing types keep a name. ecj implements that recovery for
simple names (`ParameterizedSingleTypeReference`, via `closestMatch()`) and for raw qualified names
(`QualifiedTypeReference.getTypeBinding`, via `environment.createMissingType(...)`), but **not** for
`ParameterizedQualifiedTypeReference`. Its `internalResolveLeafType` reports the problem, resolves the type arguments
"to be resilient", and returns `null` without creating a missing type (ecj 3.46.100 /
`org.eclipse.jdt.core` 3.47.0). This happens whether or not the package exists on the sourcepath.

Spellings, with no classpath:

| Spelling | Result |
| --- | --- |
| `Missing`, `Missing<String>` (imported or not) | recovered, name kept |
| `a.b.Missing` | recovered, name kept |
| `a.b.Missing<String>` | **lost** |
| `Outer.Inner<String>` with `import x.Outer;` | **lost** |
| `Outer<String>.Inner` | **lost** |
| `a.b.Missing<?>[]`, `java.util.List<a.b.Missing<String>>`, `Map.Entry<K, a.b.M<V>>` | **lost** |

### What reaches `JdtApiVisitor`

| Position | Effect |
| --- | --- |
| Field, method, constructor, annotation member whose signature contains a lost type | the member's binding is `null`; it is absent from `getDeclaredFields()` / `getDeclaredMethods()` |
| Superclass | `binding.getSuperclass()` is `java.lang.Object` |
| Implemented / extended interfaces | dropped from `binding.getInterfaces()` |
| Type parameter bounds (type or method) | bound becomes `Object`; the member survives with a wrong erasure (`m(Object)` instead of `m(a.b.Missing)`) |
| Record component | accessor dropped (even when declared explicitly), and the canonical constructor binding (`isCanonicalConstructor() == true`) is **synthesized without the component**: `record R(a.b.M<String> c, int i)` gets `R(int)`. This holds for implicit, compact and explicit canonical constructors. |
| Interface members | dropped like other members (JDT bindings normally carry the implicit `public`/`abstract`/`static final`) |
| Enum abstract methods | dropped; the enum keeps `abstract` in JDT's modifiers, but `hasAbstractEnumMethods` no longer sees the method |

On the DOM side, the `ParameterizedType` node of a lost type has a recovered binding named after a prefix of the
written name (e.g. `com.example`), with `isParameterizedType() == false`. Its type arguments still have correct
bindings. When the base resolves but an argument is lost (`List<a.b.M<String>>`), the node's binding is the raw base
(`java.util.List`, not parameterized).

### Consequences

- False negatives: removed methods, fields, constructors, record components, supertypes; changed bounds; changed
  type arguments of parameters.
- Wrong kinds: a return type changing from `String` to `a.b.M<String>` is reported as `EXECUTABLE_REMOVED` instead of
  `METHOD_RETURN_TYPE_ERASURE_CHANGED` + `METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE`.
- Possible false positives from the made-up record constructor.
- `getUnresolvedTypes()` never contains the lost types, so `checkFullyResolved()` (`--fail-on-unresolved`, Maven
  `failOnUnresolvedTypes`) passes and the `Roseau.diff` warning is silent.

### Why `--fail-on-unresolved` misses it today

Unresolved types are collected lazily by `CachingTypeResolver` when the analysis resolves a reference, and
`RoseauReport.missingFromClasspath` further keeps only names that appear as references in the extracted model. A lost
type is neither: it never entered the model. Both filters are by construction blind to extraction losses.

## Goal and non-goals

Goals:

1. Every API-relevant declaration that JDT did not extract faithfully is recorded as an **extraction gap**, with the
   best name Roseau can give the offending type and its source location.
2. In-scope gaps are always logged as a warning, and make `checkFullyResolved()` fail, so `--fail-on-unresolved` is
   reliable.
3. No false gaps on sources that compile with their full classpath.

Non-goals:

- Recovering lost members, supertypes, bounds, or records (rejected: too complex). The model stays as JDT gives it,
  except for removing the made-up record constructor (see below).
- Changing the lazy semantics for references that *are* faithfully modeled: a recovered `a.b.Missing` is still reported
  only when the analysis needs it. Gaps are different because what is missing is the declaration itself.
- Fixing ambiguous names: `a.b.Outer.Inner` may be a package or a nested type; without a classpath this is
  undecidable and stays as written.
- The ASM backend: bytecode descriptors and signatures are complete, so ASM extraction never produces gaps.

## Design

### 1. Gap model

A parser-independent record in `api/model/` (no JDT types, JSON-serializable, structural equality), e.g.:

```java
public record ExtractionGap(String declaringType, String declaration, String reference, SourceLocation location,
                            Reason reason) {
	public enum Reason { UNRESOLVED_TYPE, NO_BINDING, UNPARSED_FILE, UNKNOWN_CONSTANT }
}
```

- `declaringType`: qualified name of the type whose declaration is lossy (`pkg.A`, `pkg.A$B`), or `null` for file-level
  gaps.
- `declaration`: human-readable symbol (`pkg.A.f`, `pkg.A.m(…)`, `pkg.A` for headers).
- `reference`: best-effort qualified name of the type that could not be resolved (`com.example.absent.Missing`), or
  `null` when there is none (malformed declaration, unparsed file).
- `location`: file relative to the library root and line of the offending node.

Names are suggestions; match neighboring model classes.

### 2. Detection in `JdtApiVisitor`

**Broken type predicate.** A `Type` node is *broken* if its binding is `null`, or if it is a `ParameterizedType` whose
binding is not a parameterized type, or if any child type node (base, type argument, wildcard bound, array element,
qualifier of a `QualifiedType`) is broken. This is the single signal for ecj's failure; it never triggers on resolved
or properly recovered types (`List<String>`, `Missing<String>`, `a.b.Missing`).

**Positions checked**, only for declarations that can be part of the API (not `private`, not in local or anonymous
classes):

| Declaration | Checked nodes |
| --- | --- |
| Class / interface / enum / record header | `getSuperclassType()`, `superInterfaceTypes()`, type parameter bounds |
| Field | the field type (each fragment, including extra dimensions) |
| Method / constructor | return type, parameter types (including varargs), type parameter bounds, thrown types |
| Annotation member | return type |
| Record component | component type |

Rules:

- A declaration with a broken node yields one `UNRESOLVED_TYPE` gap per broken base name.
- A non-private field fragment, method, constructor or annotation member whose `resolveBinding()` is `null` and that has
  no broken node yields a `NO_BINDING` gap (other ecj losses, e.g. malformed or duplicate declarations). Visibility
  comes from the AST modifiers plus the implicit rules for interface members (members of interfaces are public unless
  declared `private`).
- A type declaration without a binding (today: `LOGGER.warn("No binding for {}; skipping")`) yields a `NO_BINDING` gap,
  unless it is local, anonymous or explicitly `private`.
- Records: when any component is broken, also drop the binding whose `isCanonicalConstructor()` is true. It does not
  exist in the source and would produce false breaking changes. The component's gap already reports the loss.

Nested type declarations are visited separately and report under their own qualified name.

**Reference names.** Use the written base name of the broken node (`ParameterizedType.getType()`, ignoring type
arguments, the qualifier's type arguments and type annotations) and qualify it with the visitor's existing import
rules: a single-segment name goes through `resolveUnresolvedSimpleName`; a dotted name whose first segment has a
single-type import becomes `<import>$<rest joined with $>` (`Outer.Inner` + `import x.Outer;` → `x.Outer$Inner`);
any other dotted name stays as written. When the base node itself has a real (non-recovered) binding, as in
`java.util.Map.Entry<K, a.b.M<V>>`, only the lost argument is reported.

Detection only reads the AST nodes the visitor already has (`processAbstractTypeDeclaration` holds the declaration
node; `type.bodyDeclarations()` gives members and their bindings). Cost is one walk over signature type nodes per
declaration.

### 3. Unparsed compilation units (`JdtTypesExtractor`)

`parseTypes` submits `sourcesArray` to `createASTs` but never checks that `acceptAST` was called for each file. A
project memory (2026-09-05) records that JDT can stop delivering units once the visitor runs inside `acceptAST`
(spring-web 6.2.2: 0 of 1028 types; spring-context 6.2.12: 5 of 727), with no error. Re-check that it still reproduces
with `PopularLibrariesTestIT` fixtures. Either way, record an `UNPARSED_FILE` gap for every submitted file that was
never accepted. This is cheap and closes the worst silent loss. Consider making it a hard error even without
`--fail-on-unresolved`, since a truncated model makes every report meaningless.

### 4. Compile-time constants (last phase; verify first)

Suspected: a `static final` primitive or `String` field whose initializer references a missing type
(`= Missing.VALUE`) gets `getConstantValue() == null`, so `compileTimeConstant` is silently `false` and constant-value
changes are missed. Before implementing, confirm with a probe. If confirmed: record an `UNKNOWN_CONSTANT` gap when such
a field has an initializer, no constant value, and a JDT problem inside the initializer's source range. Non-constant
initializers (`= compute()`) produce no problem and no gap.

### 5. Storage and incremental extraction

- `ExtractorSink` collects gaps alongside types; `parseTypes`' result carries them.
- `LibraryTypes` holds an immutable set of gaps, serialized with the snapshot (`@JsonProperty`), preserved by JSON
  round-trips and part of equality. ASM-built `LibraryTypes` have none.
- `IncrementalJdtTypesExtractor` keeps the gaps whose file is neither deleted nor updated and adds the gaps of
  re-parsed files, mirroring how it keeps `TypeDecl`s by `getLocation().file()`. Full re-extraction paths (classpath or
  `module-info.java` change) recompute everything.

### 6. Scope and reporting

A gap matters only if it can affect the exported API:

- in scope if its declaring type is exported, or is a supertype (transitively) of an exported type, because inherited
  public members of non-exported supertypes are part of the API;
- in scope if its declaring type is absent from the model (lost type declaration) or it is file-level, conservatively;
- out of scope otherwise (internal code with missing dependencies must not make the flag unusable).

Reporting:

- `API` exposes the in-scope gaps (e.g. `getExtractionGaps()`). `API.getUnresolvedTypes()` returns the resolver's
  unresolved types plus the `reference` names of in-scope gaps.
- `RoseauReport.getUnresolvedTypes()` adds the in-scope gap references of both versions **without** the
  `missingFromClasspath` "referenced in the model" intersection, minus gaps matched by the report's exclusions (same
  name-pattern / annotation predicate used for breaking changes).
- `checkFullyResolved()` fails if there is any unresolved type or in-scope gap. The message lists unresolved types as
  today, then the gaps with their declaration and location, e.g.
  `2 declaration(s) could not be extracted: pkg.A.f (pkg/A.java:3, com.example.absent.Missing), …`. It caps the list
  as today and ends with the classpath hint.
- The `Roseau.diff` warning includes gaps. `RoseauCLI` and `RoseauMojo` already call `checkFullyResolved()`; only their
  messages and docs change.

## Tests

Disposition of the tests on this branch:

- `UnresolvedTypesTest` (4 new tests): keep as is; they hold under this design because gap references feed
  `getUnresolvedTypes()`.
- `extractors/jdt/QualifiedParameterizedUnresolvedTypesTest` (12 tests asserting recovery): rewrite to assert one gap
  per position (declaring type, declaration, reference `com.example.absent.Missing`, line), using the same snippets.
- `diff/QualifiedParameterizedUnresolvedTypesDiffTest` (9 tests asserting recovered breaking changes): rewrite to assert
  that `checkFullyResolved()` throws and names `unknown.Unknown`. The expected breaking changes, validated against the
  imported spelling and the JLS, are recorded below in case recovery is revisited.
- `JdtTypesExtractorTest.unresolved_symbols_have_fqns` (comment "JDT can't parse … n(unknown.C<U> p1, …)"): keep the
  single-method assertion and add the gap for `n` with reference `unknown.C`.

New coverage:

- Spellings: `Outer.Inner<String>` with an imported `Outer` (→ `x.Outer$Inner`), `Outer<String>.Inner`,
  `a.b.Missing<?>[]`, `java.util.Map.Entry<String, a.b.M<String>>` (only `a.b.M` reported). Import cases must write
  files like `JdtTypesExtractorTest`: `TestUtils.buildSourcesMap` drops everything above a type declaration, imports
  included.
- Positions: interface field/method/default method, enum abstract method, annotation member
  (`Class<? extends a.b.M<String>> value()`), record with implicit, compact and explicit canonical constructors (gap
  reported and made-up `R(int)` absent).
- Scope: `private` member → no gap; broken member of a non-exported type → not reported; broken public member of a
  non-exported superclass of an exported class → reported; excluded type → not reported.
- No regression in lazy semantics: identical raw `a.b.Missing` fields in both versions still do not fail.
- `NO_BINDING`: malformed field (`public int float double f;`, existing test) → gap.
- JSON round-trip of `LibraryTypes` with gaps; incremental extraction keeps gaps of unchanged files and drops them when
  the file is fixed.
- CLI: `RoseauCLITest` with `--fail-on-unresolved` on a qualified parameterized fixture → failure exit code and
  message. Maven IT optional.

Acceptance criteria:

- The full core suite and smoke tests (real libraries with their classpath) produce zero gaps.
- `PopularLibrariesTestIT` / spring fixtures: truncation, if it still occurs, produces `UNPARSED_FILE` gaps.

## Docs

Update `docs/reference/cli.md`, `docs/reference/maven-plugin.md`, `docs/guides/ci.md`, `docs/guides/config.md` and
`docs/guides/maven-plugin.md`: `--fail-on-unresolved` / `failOnUnresolvedTypes` also fails when declarations could not
be extracted from sources, and the warning lists them. Update `cli/src/test/resources/expected/help.txt` if the option
description changes.

## Out of scope and known limits

- Recovery of lost declarations (non-goal). An upstream ecj fix (missing-type recovery in
  `ParameterizedQualifiedTypeReference`, like `QualifiedTypeReference`) would make gaps disappear naturally; worth
  reporting to eclipse-jdt.
- Pre-existing naming imprecision for faithful references: raw `Outer.Inner` with `import x.Outer;` is recorded as
  `Outer.Inner` instead of `x.Outer$Inner`. It is still reported when needed, under that name. Fixing it would reuse
  the reference-naming rule above but changes model names, so it is a separate change.
- Ambiguous dotted names stay as written.

## Appendix: expected breaking changes if recovery were implemented

Validated against the same snippets written with `import unknown.Unknown;` and `Unknown<…>` (which Roseau handles
correctly), and against the JLS. Lines are for the snippets as written in
`QualifiedParameterizedUnresolvedTypesDiffTest`.

| v1 → v2 | Expected |
| --- | --- |
| remove `m(unknown.Unknown<String>)` | `A.m(unknown.Unknown<java.lang.String>)` `EXECUTABLE_REMOVED` (2) |
| remove field `unknown.Unknown<String> f` | `A.f` `FIELD_REMOVED` (2) |
| remove constructor `A(unknown.Unknown<String>)` | `A.<init>(unknown.Unknown<java.lang.String>)` `EXECUTABLE_REMOVED` (3) |
| `record A(unknown.Unknown<String> c, int i)` → `record A(int i)` | `A.<init>(unknown.Unknown<java.lang.String>,int)` and `A.c()` `EXECUTABLE_REMOVED` (-1) |
| parameter `Unknown<String>` → `Unknown<Integer>` | `EXECUTABLE_PARAMETER_GENERICS_CHANGED` (2) |
| return `String` → `unknown.Unknown<String>` | `METHOD_RETURN_TYPE_ERASURE_CHANGED` + `METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE` (2); incompatible for sure because `String` is final |
| drop `extends unknown.Unknown<String>` / `implements unknown.Unknown<String>` | `A` `TYPE_SUPERTYPE_REMOVED` (1) |
| bound `T extends Unknown<String>` → `T extends Unknown<Integer>` | `A` `FORMAL_TYPE_PARAMETER_CHANGED` (1) |

## Reproduction

Probe with JDT directly (classpath from `./mvnw -pl core dependency:build-classpath`), parsing with the same settings
as `JdtTypesExtractor` (bindings, bindings recovery, ignored method bodies, no classpath, sources root as sourcepath):

```java
// A.java
import com.example.absent.Outer;
public class A extends com.example.absent.Missing<String> {   // superclass -> java.lang.Object
	public com.example.absent.Missing<String> f;              // no binding: field dropped
	public com.example.absent.Missing raw;                    // recovered: OK
	public Outer.Inner<String> g;                             // no binding: field dropped
	public <T extends com.example.absent.Missing<String>> void m(T t) {}  // kept, bound -> Object
}
// R.java
public record R(a.b.M<String> c, int i) {}                    // canonical constructor binding is R(int); c() dropped
```

Failing tests: `./mvnw -pl core -Dtest='QualifiedParameterizedUnresolvedTypes*Test,UnresolvedTypesTest' test`
(25 failures on `8be12b0c`).
