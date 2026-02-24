package fixture.full;

@java.lang.annotation.Target(java.lang.annotation.ElementType.LOCAL_VARIABLE)
public @interface BoundedClassMarker {
	Class<? extends Number> value();
}
