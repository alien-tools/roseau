# Breaking Change Kinds

Use this page when you need the full list of breaking changes Roseau detects.

Compatibility labels:

- `Binary`: breaks already compiled clients
- `Source`: breaks recompilation
- `Both`: breaks both

These examples are intentionally minimal. One library change can trigger more than one breaking change kind, so a real diff may report several related findings for the same edit.

!!! note
    Each entry shows the smallest library change that triggers the breaking change, plus a small example of client code affected by it.

Jump to:

- [Summary](#summary)
- [Type-Related](#type-related)
- [Class-Related](#class-related)
- [Annotation-Related](#annotation-related)
- [Field-Related](#field-related)
- [Method-Related](#method-related)
- [Constructor-Related](#constructor-related)
- [Formal Type Parameters](#formal-type-parameters)

## Summary

| Kind | Compatibility | Trigger |
| --- | --- | --- |
| `TYPE_REMOVED` | Both | An exported type disappears or becomes inaccessible |
| `TYPE_NOW_PROTECTED` | Both | A public nested type becomes protected |
| `TYPE_KIND_CHANGED` | Both | A type changes kind, such as class to interface or record |
| `TYPE_SUPERTYPE_REMOVED` | Both | An exported superclass or superinterface is no longer in the type hierarchy |
| `TYPE_NEW_ABSTRACT_METHOD` | Source | An abstract class or interface gains a new abstract method |
| `CLASS_NOW_ABSTRACT` | Both | A concrete class becomes abstract |
| `CLASS_NOW_FINAL` | Both | A class becomes final or effectively final |
| `CLASS_NOW_CHECKED_EXCEPTION` | Source | An unchecked exception becomes a checked exception |
| `CLASS_NOW_STATIC` | Both | A nested inner class becomes static |
| `CLASS_NO_LONGER_STATIC` | Both | A nested static class becomes inner |
| `ANNOTATION_TARGET_REMOVED` | Source | An annotation loses one or more legal targets |
| `ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT` | Source | An annotation adds an element with no default value |
| `ANNOTATION_NO_LONGER_REPEATABLE` | Source | An annotation stops being repeatable |
| `ANNOTATION_METHOD_NO_LONGER_DEFAULT` | Source | An annotation element loses its default value |
| `FIELD_REMOVED` | Both | A visible field disappears |
| `FIELD_NOW_PROTECTED` | Both | A public field becomes protected |
| `FIELD_TYPE_ERASURE_CHANGED` | Binary | A field type change alters the erased field descriptor |
| `FIELD_TYPE_CHANGED_INCOMPATIBLE` | Source | A field type change breaks source-compatible reads or writes |
| `FIELD_NOW_FINAL` | Both | A writable field becomes final |
| `FIELD_NOW_STATIC` | Binary | An instance field becomes static |
| `FIELD_NO_LONGER_STATIC` | Both | A static field becomes instance |
| `METHOD_REMOVED` | Both | A visible method disappears |
| `METHOD_NOW_PROTECTED` | Both | A public method becomes protected |
| `METHOD_RETURN_TYPE_ERASURE_CHANGED` | Binary | A method return type change alters the erased signature |
| `METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE` | Source | A method return type change breaks callers or overriders |
| `METHOD_NOW_ABSTRACT` | Both | A concrete method becomes abstract |
| `METHOD_NOW_FINAL` | Both | An overridable method becomes final |
| `METHOD_NOW_STATIC` | Binary | An instance method becomes static |
| `METHOD_OVERRIDABLE_NOW_STATIC` | Source | An overridable or interface method becomes static |
| `METHOD_NO_LONGER_STATIC` | Both | A static method becomes instance |
| `METHOD_NOW_THROWS_CHECKED_EXCEPTION` | Source | A method or constructor adds or widens checked exceptions |
| `METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION` | Source | A method or constructor removes or narrows checked exceptions |
| `METHOD_PARAMETER_GENERICS_CHANGED` | Source | Parameter generic arguments change while erasure stays the same |
| `CONSTRUCTOR_REMOVED` | Both | A visible constructor disappears |
| `CONSTRUCTOR_NOW_PROTECTED` | Both | A public constructor becomes protected |
| `FORMAL_TYPE_PARAMETER_ADDED` | Source | A generic arity increase breaks source uses |
| `FORMAL_TYPE_PARAMETER_REMOVED` | Source | A generic arity decrease breaks source uses |
| `FORMAL_TYPE_PARAMETER_CHANGED` | Source | Generic bounds change incompatibly |

## Type-Related

### `TYPE_REMOVED`

Compatibility: `Both`

Detection logic: Roseau reports this when an exported type disappears from the public API or becomes inaccessible.

**Library change**

```diff
-public class A {}
```

**Example client code**

```java
new A();
```

Impact: client code can no longer reference `A`.

### `TYPE_NOW_PROTECTED`

Compatibility: `Both`

Detection logic: Roseau reports this when a public nested type becomes protected.

**Library change**

```diff
 public class A {
-  public class B {}
+  protected class B {}
 }
```

**Example client code**

```java
A.B b;
```

Impact: code outside the allowed inheritance context can no longer access `A.B`.

### `TYPE_KIND_CHANGED`

Compatibility: `Both`

Detection logic: Roseau reports this when a type changes kind, for example from a class to an interface, enum, or record.

**Library change**

```diff
-public class A {}
+public interface A {}
```

**Example client code**

```java
A a = new A();
```

Impact: the old construction and usage model no longer applies.

### `TYPE_SUPERTYPE_REMOVED`

Compatibility: `Both`

Detection logic: Roseau computes the exported supertypes of the old API and reports the closest one that is no longer present in the new hierarchy.

**Library change**

```diff
 public class A {}
-public class B extends A {}
+public class B {}
```

**Example client code**

```java
A a = new B();
```

Impact: upcasts and API contracts based on the old supertype stop working.

### `TYPE_NEW_ABSTRACT_METHOD`

Compatibility: `Source`

Detection logic: Roseau reports this when an abstract class or interface gains a new abstract method that implementers must provide.

**Library change**

```diff
-public interface I {}
+public interface I {
+  void m();
+}
```

**Example client code**

```java
I i = new I() {};
```

Impact: existing implementations or anonymous classes must now implement `m()`.

## Class-Related

### `CLASS_NOW_ABSTRACT`

Compatibility: `Both`

Detection logic: Roseau reports this when a concrete class becomes abstract and can no longer be instantiated directly.

**Library change**

```diff
-public class A {}
+public abstract class A {}
```

**Example client code**

```java
new A();
```

Impact: direct instantiation stops working.

### `CLASS_NOW_FINAL`

Compatibility: `Both`

Detection logic: Roseau reports this when a class becomes final or effectively final, which blocks subclassing.

**Library change**

```diff
-public class A {}
+public final class A {}
```

**Example client code**

```java
class B extends A {}
```

Impact: the client can no longer extend `A`.

### `CLASS_NOW_CHECKED_EXCEPTION`

Compatibility: `Source`

Detection logic: Roseau reports this when an exception type that used to be unchecked becomes checked.

**Library change**

```diff
-public class A extends RuntimeException {}
+public class A extends Exception {}
```

**Example client code**

```java
throw new A();
```

Impact: callers now need to catch or declare `A`.

### `CLASS_NOW_STATIC`

Compatibility: `Both`

Detection logic: Roseau reports this when a nested inner class becomes static.

**Library change**

```diff
 public class A {
-  public class B {}
+  public static class B {}
 }
```

**Example client code**

```java
new A().new B();
```

Impact: the old qualified instantiation syntax is no longer valid.

### `CLASS_NO_LONGER_STATIC`

Compatibility: `Both`

Detection logic: Roseau reports this when a nested static class becomes inner.

**Library change**

```diff
 public class A {
-  public static class B {}
+  public class B {}
 }
```

**Example client code**

```java
new A.B();
```

Impact: clients now need an instance of `A` to create `B`.

## Annotation-Related

### `ANNOTATION_TARGET_REMOVED`

Compatibility: `Source`

Detection logic: Roseau reports this when an annotation loses one or more valid `@Target` locations.

**Library change**

```diff
 @java.lang.annotation.Target({
-  java.lang.annotation.ElementType.FIELD,
-  java.lang.annotation.ElementType.LOCAL_VARIABLE
+  java.lang.annotation.ElementType.FIELD
 })
 public @interface A {}
```

**Example client code**

```java
@A int i;
```

Impact: annotation uses at removed targets no longer compile.

### `ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT`

Compatibility: `Source`

Detection logic: Roseau reports this when an annotation gains a new element without a default value.

**Library change**

```diff
 public @interface A {
   int i();
+  String s();
 }
```

**Example client code**

```java
@A(i = 0) int a;
```

Impact: clients must now provide a value for `s`.

### `ANNOTATION_NO_LONGER_REPEATABLE`

Compatibility: `Source`

Detection logic: Roseau reports this when an annotation loses `@Repeatable`.

**Library change**

```diff
-@java.lang.annotation.Repeatable(Container.class)
 public @interface A {}
```

**Example client code**

```java
@A @A int a;
```

Impact: repeated uses of `@A` no longer compile.

### `ANNOTATION_METHOD_NO_LONGER_DEFAULT`

Compatibility: `Source`

Detection logic: Roseau reports this when an existing annotation element loses its default value.

**Library change**

```diff
 public @interface A {
-  String value() default "";
+  String value();
 }
```

**Example client code**

```java
@A int a;
```

Impact: clients must now provide `value`.

## Field-Related

### `FIELD_REMOVED`

Compatibility: `Both`

Detection logic: Roseau reports this when a visible field disappears from the API.

**Library change**

```diff
 public class A {
-  public int f;
 }
```

**Example client code**

```java
int x = new A().f;
```

Impact: field access no longer resolves.

### `FIELD_NOW_PROTECTED`

Compatibility: `Both`

Detection logic: Roseau reports this when a public field becomes protected.

**Library change**

```diff
 public class A {
-  public int f;
+  protected int f;
 }
```

**Example client code**

```java
int x = new A().f;
```

Impact: code outside subclasses or the package loses access.

### `FIELD_TYPE_ERASURE_CHANGED`

Compatibility: `Binary`

Detection logic: Roseau reports this when a field type change alters the erased field descriptor. Compile-time constants are excluded.

**Library change**

```diff
 public class A {
-  public int f;
+  public Integer f;
 }
```

**Example client code**

```java
int x = new A().f;
```

Impact: recompilation can still work because of boxing, but already compiled clients expect the old field descriptor.

### `FIELD_TYPE_CHANGED_INCOMPATIBLE`

Compatibility: `Source`

Detection logic: Roseau reports this when a field type change makes reads or writes source-incompatible, even if the erased descriptor is unchanged.

**Library change**

```diff
 public class A {
-  public java.util.List<String> f;
+  public java.util.List<Integer> f;
 }
```

**Example client code**

```java
java.util.List<String> xs = new A().f;
```

Impact: source code using the old field type no longer type-checks.

### `FIELD_NOW_FINAL`

Compatibility: `Both`

Detection logic: Roseau reports this when a writable field becomes final.

**Library change**

```diff
 public class A {
-  public int f;
+  public final int f = 0;
 }
```

**Example client code**

```java
new A().f = 1;
```

Impact: clients can no longer assign to the field.

### `FIELD_NOW_STATIC`

Compatibility: `Binary`

Detection logic: Roseau reports this when an instance field becomes static.

**Library change**

```diff
 public class A {
-  public int f;
+  public static int f;
 }
```

**Example client code**

```java
A a = new A();
int x = a.f;
```

Impact: source code can often still compile, but precompiled clients expect an instance field access.

### `FIELD_NO_LONGER_STATIC`

Compatibility: `Both`

Detection logic: Roseau reports this when a static field becomes an instance field.

**Library change**

```diff
 public class A {
-  public static int f;
+  public int f;
 }
```

**Example client code**

```java
int x = A.f;
```

Impact: clients now need an instance of `A`.

## Method-Related

### `METHOD_REMOVED`

Compatibility: `Both`

Detection logic: Roseau reports this when a visible method disappears from the API.

**Library change**

```diff
 public class A {
-  public void m() {}
 }
```

**Example client code**

```java
new A().m();
```

Impact: the old call no longer resolves.

### `METHOD_NOW_PROTECTED`

Compatibility: `Both`

Detection logic: Roseau reports this when a public method becomes protected.

**Library change**

```diff
 public class A {
-  public void m() {}
+  protected void m() {}
 }
```

**Example client code**

```java
new A().m();
```

Impact: callers outside the allowed scope lose access.

### `METHOD_RETURN_TYPE_ERASURE_CHANGED`

Compatibility: `Binary`

Detection logic: Roseau reports this when a method return type change alters the erased signature.

**Library change**

```diff
 public class A {
-  public final int m() { return 0; }
+  public final Integer m() { return 0; }
 }
```

**Example client code**

```java
int x = new A().m();
```

Impact: recompilation may still work because of boxing, but old binaries are linked against the old method descriptor.

### `METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE`

Compatibility: `Source`

Detection logic: Roseau reports this when the new return type is no longer source-compatible for callers or overrides, even when erasure stays the same.

**Library change**

```diff
 public class A<T, U> {
-  public T m() { return null; }
+  public U m() { return null; }
 }
```

**Example client code**

```java
Integer x = new A<Integer, String>().m();
```

Impact: source code using the old return type no longer type-checks.

### `METHOD_NOW_ABSTRACT`

Compatibility: `Both`

Detection logic: Roseau reports this when a concrete method becomes abstract.

**Library change**

```diff
 public abstract class A {
-  public void m() {}
+  public abstract void m();
 }
```

**Example client code**

```java
new A() {};
```

Impact: subclasses or anonymous classes must now implement `m()`.

### `METHOD_NOW_FINAL`

Compatibility: `Both`

Detection logic: Roseau reports this when an overridable method becomes final.

**Library change**

```diff
 public class A {
-  public void m() {}
+  public final void m() {}
 }
```

**Example client code**

```java
class B extends A {
  @Override public void m() {}
}
```

Impact: overriding `m()` is no longer legal.

### `METHOD_NOW_STATIC`

Compatibility: `Binary`

Detection logic: Roseau reports this when an instance method becomes static.

**Library change**

```diff
 public final class A {
-  public void m() {}
+  public static void m() {}
 }
```

**Example client code**

```java
new A().m();
```

Impact: source code can still compile in many cases, but precompiled clients use the wrong invocation mode.

### `METHOD_OVERRIDABLE_NOW_STATIC`

Compatibility: `Source`

Detection logic: Roseau reports this in addition to `METHOD_NOW_STATIC` when the old method was overridable or declared in an interface.

**Library change**

```diff
 public class A {
-  public void m() {}
+  public static void m() {}
 }
```

**Example client code**

```java
class B extends A {
  @Override public void m() {}
}
```

Impact: the override no longer compiles because `m()` is now static.

### `METHOD_NO_LONGER_STATIC`

Compatibility: `Both`

Detection logic: Roseau reports this when a static method becomes an instance method.

**Library change**

```diff
 public class A {
-  public static void m() {}
+  public void m() {}
 }
```

**Example client code**

```java
A.m();
```

Impact: callers now need an instance of `A`.

### `METHOD_NOW_THROWS_CHECKED_EXCEPTION`

Compatibility: `Source`

Detection logic: Roseau reports this when a method or constructor adds a new checked exception or widens its checked-exception contract.

**Library change**

```diff
 public class A {
-  public void m() {}
+  public void m() throws java.io.IOException {}
 }
```

**Example client code**

```java
new A().m();
```

Impact: callers must catch or declare the checked exception.

### `METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION`

Compatibility: `Source`

Detection logic: Roseau reports this when a method or constructor removes a checked exception or narrows the checked-exception contract in a breaking way.

**Library change**

```diff
 public class A {
-  public void m() throws java.io.IOException {}
+  public void m() {}
 }
```

**Example client code**

```java
try {
  new A().m();
} catch (java.io.IOException e) {}
```

Impact: the catch block becomes unreachable, and override contracts can also break.

### `METHOD_PARAMETER_GENERICS_CHANGED`

Compatibility: `Source`

Detection logic: Roseau reports this when parameter generic arguments change while the erased signature stays the same. Overridable methods are treated invariantly; effectively final executables get a variance-based check.

**Library change**

```diff
 public class A {
-  public final void m(java.util.List<String> xs) {}
+  public final void m(java.util.List<Integer> xs) {}
 }
```

**Example client code**

```java
new A().m(java.util.List.of("x"));
```

Impact: source calls using the old generic parameter type no longer compile.

## Constructor-Related

### `CONSTRUCTOR_REMOVED`

Compatibility: `Both`

Detection logic: Roseau reports this when a visible constructor disappears from the API.

**Library change**

```diff
 public class A {
-  public A() {}
+  public A(int i) {}
 }
```

**Example client code**

```java
new A();
```

Impact: the old construction call no longer resolves.

### `CONSTRUCTOR_NOW_PROTECTED`

Compatibility: `Both`

Detection logic: Roseau reports this when a public constructor becomes protected.

**Library change**

```diff
 public class A {
-  public A() {}
+  protected A() {}
 }
```

**Example client code**

```java
new A();
```

Impact: callers outside the allowed scope lose access to the constructor.

## Formal Type Parameters

### `FORMAL_TYPE_PARAMETER_ADDED`

Compatibility: `Source`

Detection logic: Roseau reports this when a type, method, or constructor gains an extra formal type parameter in a way that changes generic arity.

**Library change**

```diff
-public class A<T> {}
+public class A<T, U> {}
```

**Example client code**

```java
A<String> a;
```

Impact: the old generic arity is no longer valid.

### `FORMAL_TYPE_PARAMETER_REMOVED`

Compatibility: `Source`

Detection logic: Roseau reports this when a type, method, or constructor loses a formal type parameter in a way that changes generic arity.

**Library change**

```diff
-public class A<T, U> {}
+public class A<T> {}
```

**Example client code**

```java
A<String, Integer> a;
```

Impact: source code using the old number of type arguments no longer compiles.

### `FORMAL_TYPE_PARAMETER_CHANGED`

Compatibility: `Source`

Detection logic: Roseau reports this when generic bounds become stricter or otherwise incompatible. For types, each new bound must remain compatible with an old one. For executables, Roseau uses stricter rules for overridable members.

**Library change**

```diff
-public class A<T extends CharSequence> {}
+public class A<T extends String> {}
```

**Example client code**

```java
A<StringBuilder> a = new A<>();
```

Impact: previously valid type arguments may no longer satisfy the new bounds.
