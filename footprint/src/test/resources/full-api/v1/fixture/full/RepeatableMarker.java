package fixture.full;

@java.lang.annotation.Target({
	java.lang.annotation.ElementType.LOCAL_VARIABLE,
	java.lang.annotation.ElementType.METHOD
})
@java.lang.annotation.Repeatable(RepeatableMarker.Container.class)
public @interface RepeatableMarker {
	String value() default "repeatable";

	@java.lang.annotation.Target({
		java.lang.annotation.ElementType.LOCAL_VARIABLE,
		java.lang.annotation.ElementType.METHOD
	})
	@interface Container {
		RepeatableMarker[] value();
	}
}
