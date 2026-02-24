package fixture.full;

@java.lang.annotation.Target(java.lang.annotation.ElementType.PACKAGE)
public @interface PackageOnlyMarker {
	String value() default "pkg";
}
