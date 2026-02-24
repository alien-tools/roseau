package fixture.full;

@java.lang.annotation.Target(java.lang.annotation.ElementType.ANNOTATION_TYPE)
public @interface MetaOnlyMarker {
	String value() default "meta";
}
