# Kinds of breaking changes

This catalog lists every breaking change kind Roseau reports.

!!! warning "Detection rules"
    Roseau's detection rules aim to be as accurate as possible, avoiding false positives and false negatives. Many edge cases involving complex interactions are not covered on this page. While it describes the most common cases, the full detection rules can be [reviewed in the source code](https://github.com/alien-tools/roseau/tree/main/core/src/main/java/io/github/alien/roseau/diff/rules/breaking), with examples in [the corresponding test cases](https://github.com/alien-tools/roseau/tree/main/core/src/test/java/io/github/alien/roseau/diff).

## Conventions

**Breaking changes are expressed at the API level, not the code level.** Roseau works with the [API surface](api-surface.md) it extracted, not the raw source code or bytecode is analyzes. For example, a `public` type that becomes `package-private` is treated the same as a type that is removed from the codebase: it is reported as `TYPE_REMOVED`. From Roseau's perspective the type isn't API anymore. Likewise, changes to declarations that are not part of the API surface are ignored and not checked for breaking changes. This includes, for instance, `protected` members of effectively-final types.

Each entry shows the smallest library change that triggers the kind, and a minimal example of client code affected by it. One library edit can trigger multiple kinds, so a real diff may report several related findings for the same change.

**Compatibility icons used throughout this page:**

:material-package-variant-closed: &nbsp;**Binary** — breaks already compiled clients (`.class` files linked against the old API)

:material-code-braces: &nbsp;**Source** — breaks clients on recompilation (source code that compiled against the old API)

An absent icon means the change does not affect compatibility in that category.

---

Jump to: [Type-Related](#type-related) · [Class-Related](#class-related) · [Annotation-Related](#annotation-related) · [Field-Related](#field-related) · [Method-Related](#method-related) · [Constructor-Related](#constructor-related) · [Formal Type Parameters](#formal-type-parameters)

---

## Summary

| Kind | :material-package-variant-closed: | :material-code-braces: | Trigger                                                                     |
| --- | :---: | :---: |-----------------------------------------------------------------------------|
| [`TYPE_REMOVED`](#type_removed) | :material-package-variant-closed: | :material-code-braces: | A visible type is removed from the API                                      |
| [`TYPE_NOW_PROTECTED`](#type_now_protected) | :material-package-variant-closed: | :material-code-braces: | A public nested type becomes protected                                      |
| [`TYPE_KIND_CHANGED`](#type_kind_changed) | :material-package-variant-closed: | :material-code-braces: | A type changes its kind (e.g., changing a class to an enum or a record)     |
| [`TYPE_SUPERTYPE_REMOVED`](#type_supertype_removed) | :material-package-variant-closed: | :material-code-braces: | A visible supertype is removed from a type's hierarchy                      |
| [`TYPE_NEW_ABSTRACT_METHOD`](#type_new_abstract_method) | | :material-code-braces: | An abstract class or interface gains a new abstract method                  |
| [`CLASS_NOW_ABSTRACT`](#class_now_abstract) | :material-package-variant-closed: | :material-code-braces: | A concrete class becomes abstract                                           |
| [`CLASS_NOW_FINAL`](#class_now_final) | :material-package-variant-closed: | :material-code-braces: | A class becomes effectively final                                           |
| [`CLASS_NOW_CHECKED_EXCEPTION`](#class_now_checked_exception) | | :material-code-braces: | An unchecked exception becomes checked                                      |
| [`CLASS_NOW_STATIC`](#class_now_static) | :material-package-variant-closed: | :material-code-braces: | A nested inner class becomes static                                         |
| [`CLASS_NO_LONGER_STATIC`](#class_no_longer_static) | :material-package-variant-closed: | :material-code-braces: | A nested static class becomes inner                                         |
| [`ANNOTATION_TARGET_REMOVED`](#annotation_target_removed) | | :material-code-braces: | An annotation loses one or more legal targets                               |
| [`ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT`](#annotation_new_method_without_default) | | :material-code-braces: | An annotation gains an element with no default value                        |
| [`ANNOTATION_NO_LONGER_REPEATABLE`](#annotation_no_longer_repeatable) | | :material-code-braces: | An annotation stops being repeatable                                        |
| [`ANNOTATION_METHOD_NO_LONGER_DEFAULT`](#annotation_method_no_longer_default) | | :material-code-braces: | An annotation element loses its default value                               |
| [`FIELD_REMOVED`](#field_removed) | :material-package-variant-closed: | :material-code-braces: | A visible field is removed from the API                                     |
| [`FIELD_NOW_PROTECTED`](#field_now_protected) | :material-package-variant-closed: | :material-code-braces: | A public field becomes protected                                            |
| [`FIELD_TYPE_ERASURE_CHANGED`](#field_type_erasure_changed) | :material-package-variant-closed: | | A field type change alters the erased field descriptor                      |
| [`FIELD_TYPE_CHANGED_INCOMPATIBLE`](#field_type_changed_incompatible) | | :material-code-braces: | A field type change breaks reads or writes                                  |
| [`FIELD_NOW_FINAL`](#field_now_final) | :material-package-variant-closed: | :material-code-braces: | A writable field becomes final                                              |
| [`FIELD_NOW_STATIC`](#field_now_static) | :material-package-variant-closed: | | An instance field becomes static                                            |
| [`FIELD_NO_LONGER_STATIC`](#field_no_longer_static) | :material-package-variant-closed: | :material-code-braces: | A static field becomes an instance field                                    |
| [`METHOD_REMOVED`](#method_removed) | :material-package-variant-closed: | :material-code-braces: | A visible method is removed from the API                                    |
| [`METHOD_NOW_PROTECTED`](#method_now_protected) | :material-package-variant-closed: | :material-code-braces: | A public method becomes protected                                           |
| [`METHOD_RETURN_TYPE_ERASURE_CHANGED`](#method_return_type_erasure_changed) | :material-package-variant-closed: | | A return type change alters the erased method signature                     |
| [`METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE`](#method_return_type_changed_incompatible) | | :material-code-braces: | A return type change breaks callers or overriders                           |
| [`METHOD_NOW_ABSTRACT`](#method_now_abstract) | :material-package-variant-closed: | :material-code-braces: | A concrete method becomes abstract                                          |
| [`METHOD_NOW_FINAL`](#method_now_final) | :material-package-variant-closed: | :material-code-braces: | An overridable method becomes final                                         |
| [`METHOD_NOW_STATIC`](#method_now_static) | :material-package-variant-closed: | | An instance method becomes static                                           |
| [`METHOD_OVERRIDABLE_NOW_STATIC`](#method_overridable_now_static) | | :material-code-braces: | An overridable or interface method becomes static                           |
| [`METHOD_NO_LONGER_STATIC`](#method_no_longer_static) | :material-package-variant-closed: | :material-code-braces: | A static method becomes an instance method                                  |
| [`METHOD_NOW_THROWS_CHECKED_EXCEPTION`](#method_now_throws_checked_exception) | | :material-code-braces: | A method adds or widens a checked exception                                 |
| [`METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION`](#method_no_longer_throws_checked_exception) | | :material-code-braces: | A method removes or narrows a checked exception                             |
| [`METHOD_PARAMETER_GENERICS_CHANGED`](#method_parameter_generics_changed) | | :material-code-braces: | Parameter generic arguments change while erasure stays the same             |
| [`CONSTRUCTOR_REMOVED`](#constructor_removed) | :material-package-variant-closed: | :material-code-braces: | A visible constructor disappears                                            |
| [`CONSTRUCTOR_NOW_PROTECTED`](#constructor_now_protected) | :material-package-variant-closed: | :material-code-braces: | A public constructor becomes protected                                      |
| [`FORMAL_TYPE_PARAMETER_ADDED`](#formal_type_parameter_added) | | :material-code-braces: | A formal type parameter is added to a declaration                           |
| [`FORMAL_TYPE_PARAMETER_REMOVED`](#formal_type_parameter_removed) | | :material-code-braces: | A formal type parameter is removed from a declaration                       |
| [`FORMAL_TYPE_PARAMETER_CHANGED`](#formal_type_parameter_changed) | | :material-code-braces: | A declaration's formal type parameter changes bounds in an incompatible way |

---

## Type-Related

### `TYPE_REMOVED`

:material-package-variant-closed: :material-code-braces:

An exported type disappears from the API. This includes a `public` top-level type becoming `package-private` or a nested type no longer visible.

**Library**

```diff
-public class A {}
```

**Client**

```java
new A();
```

---

### `TYPE_NOW_PROTECTED`

:material-package-variant-closed: :material-code-braces:

A public nested type becomes protected. Code outside the allowed inheritance context can no longer reference it.

**Library**

```diff
 public class A {
-  public class B {}
+  protected class B {}
 }
```

**Client**

```java
A.B b;
```

---

### `TYPE_KIND_CHANGED`

:material-package-variant-closed: :material-code-braces:

A type changes kind — for example from a class to an interface, an enum, or a record. The rules governing how the type can be instantiated, extended, or implemented change fundamentally, breaking clients that relied on the original kind.

**Library**

```diff
-public class A {}
+public interface A {}
```

**Client**

```java
A a = new A();
```

---

### `TYPE_SUPERTYPE_REMOVED`

:material-package-variant-closed: :material-code-braces:

An exported supertype is removed from a type's hierarchy. Roseau reports the closest exported supertype that no longer appears in the new hierarchy. Upcasts and API contracts based on the removed supertype stop working.

**Library**

```diff
 public class A {}
-public class B extends A {}
+public class B {}
```

**Client**

```java
A a = new B();
```

---

### `TYPE_NEW_ABSTRACT_METHOD`

:material-code-braces:

An abstract class or interface gains a new abstract method. Existing source-level implementations must provide it.

**Library**

```diff
-public interface I {}
+public interface I {
+  void m();
+}
```

**Client**

```java
I i = new I() {};
```

!!! note "Why source-only"
    Compiled classes that already implement the interface continue to link and load — the JVM does not verify method completeness when loading a class. Only recompilation of implementing classes forces the new method to be added.

---

## Class-Related

### `CLASS_NOW_ABSTRACT`

:material-package-variant-closed: :material-code-braces:

A concrete class becomes abstract. Clients that instantiate it directly can no longer compile or link.

**Library**

```diff
-public class A {}
+public abstract class A {}
```

**Client**

```java
new A();
```

---

### `CLASS_NOW_FINAL`

:material-package-variant-closed: :material-code-braces:

A class that was not effectively final becomes effectively final, preventing subclassing. This fires for explicit `final`, for `sealed` (without `non-sealed`), and for any change that removes all constructors accessible to subclasses.

**Library**

```diff
-public class A {}
+public final class A {}
```

**Client**

```java
class B extends A {}
```

---

### `CLASS_NOW_CHECKED_EXCEPTION`

:material-code-braces:

An exception class that extended `RuntimeException` (directly or indirectly) now extends `Exception` (directly or indirectly). Callers must catch or declare it.

**Library**

```diff
-public class A extends RuntimeException {}
+public class A extends Exception {}
```

**Client**

```java
throw new A();
```

---

### `CLASS_NOW_STATIC`

:material-package-variant-closed: :material-code-braces:

A nested inner class becomes static. Source code using the qualified instantiation form `outer.new B()` no longer compiles. Compiled code also breaks: an inner class constructor carries a hidden enclosing-instance parameter that disappears when the class becomes static, so compiled call sites fail to link.

**Library**

```diff
 public class A {
-  public class B {}
+  public static class B {}
 }
```

**Client**

```java
new A().new B();
```

---

### `CLASS_NO_LONGER_STATIC`

:material-package-variant-closed: :material-code-braces:

A nested static class becomes inner. Clients now need an enclosing instance to create it, breaking both the source form and compiled code.

**Library**

```diff
 public class A {
-  public static class B {}
+  public class B {}
 }
```

**Client**

```java
new A.B();
```

---

## Annotation-Related

### `ANNOTATION_TARGET_REMOVED`

:material-code-braces:

An annotation loses one or more valid placement targets. Existing uses at those targets no longer compile.

**Library**

```diff
 @java.lang.annotation.Target({
-  java.lang.annotation.ElementType.FIELD,
-  java.lang.annotation.ElementType.LOCAL_VARIABLE
+  java.lang.annotation.ElementType.FIELD
 })
 public @interface A {}
```

**Client**

```java
@A int i;
```

---

### `ANNOTATION_NEW_METHOD_WITHOUT_DEFAULT`

:material-code-braces:

An annotation gains a new element with no default value. Existing annotation uses must now supply a value for the new element.

**Library**

```diff
 public @interface A {
   int i();
+  String s();
 }
```

**Client**

```java
@A(i = 0) int a;
```

---

### `ANNOTATION_NO_LONGER_REPEATABLE`

:material-code-braces:

An annotation loses `@Repeatable`. Sites that use the annotation more than once no longer compile.

**Library**

```diff
-@java.lang.annotation.Repeatable(Container.class)
 public @interface A {}
```

**Client**

```java
@A @A int a;
```

---

### `ANNOTATION_METHOD_NO_LONGER_DEFAULT`

:material-code-braces:

An existing annotation element loses its default value. Annotation uses that relied on the default must now supply a value explicitly.

**Library**

```diff
 public @interface A {
-  String value() default "";
+  String value();
 }
```

**Client**

```java
@A int a;
```

---

## Field-Related

### `FIELD_REMOVED`

:material-package-variant-closed: :material-code-braces:

A visible field disappears from the API and can no longer be accessed in client code.

**Library**

```diff
 public class A {
-  public int f;
 }
```

**Client**

```java
int x = new A().f;
```

---

### `FIELD_NOW_PROTECTED`

:material-package-variant-closed: :material-code-braces:

A public field becomes protected. Code outside subclasses or the package loses access.

**Library**

```diff
 public class A {
-  public int f;
+  protected int f;
 }
```

**Client**

```java
int x = new A().f;
```

---

### `FIELD_TYPE_ERASURE_CHANGED`

:material-package-variant-closed:

A field's type changes in a way that alters its erased descriptor. Recompilation can still succeed (for example via autoboxing), but compiled bytecode that references the old field descriptor fails at link time. Fields that are compile-time constants (`public static final` fields of a primitive type or `String` initialized to a constant expression) are excluded: the Java compiler inlines their value at every use site and emits no field-access instruction, so the field descriptor is irrelevant to compiled clients.

**Library**

```diff
 public class A {
-  public int f;
+  public Integer f;
 }
```

**Client**

```java
int x = new A().f;
```

!!! note "Why binary-only"
    Java source allows reading an `Integer` field into an `int` variable via unboxing, so recompilation succeeds. Compiled bytecode carries the old field descriptor (`I` vs `Ljava/lang/Integer;`) and fails when the JVM resolves the field.

---

### `FIELD_TYPE_CHANGED_INCOMPATIBLE`

:material-code-braces:

A field's type changes in a way that breaks source-level reads or writes, even though the erased descriptor is unchanged.

**Library**

```diff
 public class A {
-  public java.util.List<String> f;
+  public java.util.List<Integer> f;
 }
```

**Client**

```java
java.util.List<String> xs = new A().f;
```

---

### `FIELD_NOW_FINAL`

:material-package-variant-closed: :material-code-braces:

A writable field becomes final. Client code that assigns to the field can no longer compile or link.

**Library**

```diff
 public class A {
-  public int f;
+  public final int f = 0;
 }
```

**Client**

```java
new A().f = 1;
```

---

### `FIELD_NOW_STATIC`

:material-package-variant-closed:

An instance field becomes static. Compiled `getfield` instructions are incompatible with a static field and fail at link time.

**Library**

```diff
 public class A {
-  public int f;
+  public static int f;
 }
```

**Client**

```java
A a = new A();
int x = a.f;
```

!!! note "Why binary-only"
    Java source allows accessing a static field through an instance reference (with a warning), so recompilation usually succeeds. Compiled bytecode uses `getfield`, which is incompatible with a static field.

---

### `FIELD_NO_LONGER_STATIC`

:material-package-variant-closed: :material-code-braces:

A static field becomes an instance field. Static access no longer compiles, and compiled `getstatic` instructions fail at link time.

**Library**

```diff
 public class A {
-  public static int f;
+  public int f;
 }
```

**Client**

```java
int x = A.f;
```

---

## Method-Related

### `METHOD_REMOVED`

:material-package-variant-closed: :material-code-braces:

A visible method disappears from the API.

**Library**

```diff
 public class A {
-  public void m() {}
 }
```

**Client**

```java
new A().m();
```

---

### `METHOD_NOW_PROTECTED`

:material-package-variant-closed: :material-code-braces:

A public method becomes protected. Callers outside the allowed inheritance context lose access.

**Library**

```diff
 public class A {
-  public void m() {}
+  protected void m() {}
 }
```

**Client**

```java
new A().m();
```

---

### `METHOD_RETURN_TYPE_ERASURE_CHANGED`

:material-package-variant-closed:

A method's return type changes in a way that alters its erased signature. Recompilation can still succeed (for example via autoboxing), but compiled bytecode linked against the old method descriptor fails at link time.

**Library**

```diff
 public class A {
-  public final int m() { return 0; }
+  public final Integer m() { return 0; }
 }
```

**Client**

```java
int x = new A().m();
```

---

### `METHOD_RETURN_TYPE_CHANGED_INCOMPATIBLE`

:material-code-braces:

A method's return type changes in a way that breaks callers or overriders at the source level, even when erasure is unchanged.

Callers break when the new return type cannot be used in place of the old one — for instance, when a caller assigns the return value to a variable typed with the old return type.
Overriders break when the new return type is not a supertype of the old return type. Java's covariant override rule requires the overriding method's return type to be a subtype of the overridden method's; if the parent's return type narrows, subclasses that still declare the old (broader) type violate this rule.

**Library**

```diff
 public class A<T, U> {
-  public T m() { return null; }
+  public U m() { return null; }
 }
```

**Client**

```java
Integer x = new A<Integer, String>().m();
```

---

### `METHOD_NOW_ABSTRACT`

:material-package-variant-closed: :material-code-braces:

A concrete method becomes abstract. Subclasses or anonymous classes that do not implement it can no longer be compiled or instantiated.

**Library**

```diff
 public abstract class A {
-  public void m() {}
+  public abstract void m();
 }
```

**Client**

```java
new A() {};
```

---

### `METHOD_NOW_FINAL`

:material-package-variant-closed: :material-code-braces:

An overridable method becomes final. Existing overrides in subclasses no longer compile, and compiled override call sites fail at link time.

**Library**

```diff
 public class A {
-  public void m() {}
+  public final void m() {}
 }
```

**Client**

```java
class B extends A {
  @Override public void m() {}
}
```

---

### `METHOD_NOW_STATIC`

:material-package-variant-closed:

An instance method becomes static. Compiled `invokevirtual` instructions are incompatible with a static method and fail at link time.

**Library**

```diff
 public final class A {
-  public void m() {}
+  public static void m() {}
 }
```

**Client**

```java
new A().m();
```

!!! note "Why binary-only"
    Java source allows calling a static method through an instance reference (with a warning), so recompilation usually succeeds. Compiled bytecode uses `invokevirtual`, which is incompatible with a `static` method. See [`METHOD_OVERRIDABLE_NOW_STATIC`](#method_overridable_now_static) for the additional source-breaking case when the method was overridable.

---

### `METHOD_OVERRIDABLE_NOW_STATIC`

:material-code-braces:

An overridable method — or any interface method — becomes static. Subclasses or implementors that override it can no longer do so. Roseau reports this alongside [`METHOD_NOW_STATIC`](#method_now_static) when the old method could be overridden.

**Library**

```diff
 public class A {
-  public void m() {}
+  public static void m() {}
 }
```

**Client**

```java
class B extends A {
  @Override public void m() {}
}
```

---

### `METHOD_NO_LONGER_STATIC`

:material-package-variant-closed: :material-code-braces:

A static method becomes an instance method. Static calls no longer compile, and compiled `invokestatic` instructions fail at link time.

**Library**

```diff
 public class A {
-  public static void m() {}
+  public void m() {}
 }
```

**Client**

```java
A.m();
```

---

### `METHOD_NOW_THROWS_CHECKED_EXCEPTION`

:material-code-braces:

A method or constructor adds a checked exception that is not already covered by an existing entry in the throws clause. Callers must catch or declare it.

**Library**

```diff
 public class A {
-  public void m() {}
+  public void m() throws java.io.IOException {}
 }
```

**Client**

```java
new A().m();
```

---

### `METHOD_NO_LONGER_THROWS_CHECKED_EXCEPTION`

:material-code-braces:

A checked exception is removed or narrowed in a way that breaks overriders or callers.

- For **non-final** methods: every old checked exception must still be covered by the same or a broader exception in the new signature. Narrowing to a subtype (e.g., `IOException → FileNotFoundException`) counts as a breaking removal because subclasses that declared the old exception can no longer do so.
- For **final** methods: narrowing to a subtype is allowed; only complete removal breaks callers.

**Library**

```diff
 public class A {
-  public void m() throws java.io.IOException {}
+  public void m() {}
 }
```

**Client**

```java
try {
  new A().m();
} catch (java.io.IOException e) {}
```

!!! note "Removing an exception is a source break"
    Removing a checked exception from a non-final method is a source break for two reasons. First, subclasses that override the method may declare the same exception in their `throws` clause — once the parent removes it, those overrides no longer compile. Second, callers that catch the exception are also broken: Java makes it a compile error (not just a warning) to have a `catch` block for a checked exception that the callee can no longer throw.

---

### `METHOD_PARAMETER_GENERICS_CHANGED`

:material-code-braces:

Parameter generic arguments change while the erased signature stays the same.

- For **overridable** methods, any change to generic arguments is a source break: subclasses overriding with the old signature would have mismatched parameter types.
- For **effectively final** executables, Roseau applies a variance check: the new parameter type must be a supertype of the old to be compatible.

**Library**

```diff
 public class A {
-  public final void m(java.util.List<String> xs) {}
+  public final void m(java.util.List<Integer> xs) {}
 }
```

**Client**

```java
new A().m(java.util.List.of("x"));
```

---

## Constructor-Related

### `CONSTRUCTOR_REMOVED`

:material-package-variant-closed: :material-code-braces:

A visible constructor disappears from the API.

**Library**

```diff
 public class A {
-  public A() {}
+  public A(int i) {}
 }
```

**Client**

```java
new A();
```

---

### `CONSTRUCTOR_NOW_PROTECTED`

:material-package-variant-closed: :material-code-braces:

A public constructor becomes protected. Callers outside the allowed inheritance context can no longer invoke it.

**Library**

```diff
 public class A {
-  public A() {}
+  protected A() {}
 }
```

**Client**

```java
new A();
```

---

## Formal Type Parameters

### `FORMAL_TYPE_PARAMETER_ADDED`

:material-code-braces:

A type, method, or constructor gains an extra formal type parameter, changing its generic arity. Existing source-level uses with the old arity no longer compile.

**Library**

```diff
-public class A<T> {}
+public class A<T, U> {}
```

**Client**

```java
A<String> a;
```

---

### `FORMAL_TYPE_PARAMETER_REMOVED`

:material-code-braces:

A type, method, or constructor loses a formal type parameter, changing its generic arity. Existing source-level uses with the old arity no longer compile.

**Library**

```diff
-public class A<T, U> {}
+public class A<T> {}
```

**Client**

```java
A<String, Integer> a;
```

---

### `FORMAL_TYPE_PARAMETER_CHANGED`

:material-code-braces:

Generic bounds change incompatibly. The check depends on what is being changed:

- For **types** and **non-overridable executables** (constructors, or methods on effectively-final types): each new bound must be a supertype of some old bound. Tightening a bound — replacing it with a more specific type — narrows the set of valid type arguments and breaks existing uses.
- For **overridable methods**: bounds must be exactly equal after accounting for any type parameter renames. Any change to bounds is a break, because a subclass overriding the method with the old bounds would no longer match the new signature.

Previously valid type arguments may no longer satisfy the new bounds.

**Library**

```diff
-public class A<T extends CharSequence> {}
+public class A<T extends String> {}
```

**Client**

```java
A<StringBuilder> a = new A<>();
```
