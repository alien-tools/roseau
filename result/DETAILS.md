# Cases for all tools

## Changes not breaking at binary level

### AddModifierStaticToField
```java
public class C {
	public final int integer = 0;
	public final boolean bool = false;
	public final String string = "string";
	public final Boolean booleanObject = Boolean.TRUE;
	public final Integer integerObject = 0;
	public final Thread thread = Thread.currentThread();
}
```

### AddModifierStaticToMethod
```java
public interface I {
	void f();
}
```

### AddParameterToMethod
```java
public abstract class C {
	abstract void f();
}
```

### ChangeFieldType / ReduceFieldVisibility / RemoveModifierStatic
🤯🤯🤯🤯🤯🤯
```java
public final class Cls {
	public static final int integer = 0;
	public static final Thread thread = Thread.currentThread();
}
```

### ChangeMethodReturnType
```java
public abstract class Cls {
	public abstract int fI();
	public abstract boolean fB();
	public abstract Thread fT();
}
```

### ChangeMethodFirstParamType / RemoveMethodFirstParam
```java
public abstract class Cls {
	public abstract void f(int i);
}
```

### ReduceMethodVisibility
```java
public abstract class Cls {
	public final java.lang.Thread m1293() { return null; }
}
```

### RemoveMethod
```java
public abstract class Cls {
	abstract void m1293();
}
```

## Changes not breaking at source level

### AddParameterToFirstConstructor

By changing first constructor, clients calling new C(0) will use the varargs constructor.
```java
public class C {
    public C(int i) {}
    public C(int ...i) {}
}
```

### AddVarargsParameterToMethod

Clients calling m(0) are still valid as varargs could be empty.
```java
public class C {
    public void m(int i) {}
}
```

### ChangeMethodReturnedType

Can't extend method in those cases, so changing type has no effect as it is void in V1.
```java
public final class C {
    public void m() {}
}
public class C {
    public final void m() {}
}
public enum E {
	V1;

    public void m() {}
}
public record R() {
    public void m() {}
}
```

### ChangeParameterTypeToVarargs

Clients calling m(0) are still valid as it is corresponding to varargs type.
```java
public class C {
    public void m(int i) {}
}
```

### ReduceVisibilityOrRemoveFirstConstructor

By changing first constructor, clients calling new C(0) will use the varargs constructor.
```java
public class C {
    public C(int i) {}
    public C(int ...i) {}
}
```

### ReduceVisibilityForMethod

Not sure what is going on here
```java
public abstract class Abstr {
	public abstract void m();
	public static void mStatic() {}
	public final void mFinal() {}
}
```

### RemoveExceptionFromConstructor

Try/catch not possible to surround constructor call, so removing exception for protected constructor is not a problem.
```java
public class C {
    protected C() throws java.io.IOException {}
}
public class C2 extends C {
    public C2() throws java.io.IOException {
        super();
    }
}
```

# Cases where Roseau failed

## Changes breaking at source level

### AddModifierStaticToMethodInInterface
```java
public interface Intf {
	void f();
}
public class Client implements Intf {
	@Override
    public void f() {}
}
```
