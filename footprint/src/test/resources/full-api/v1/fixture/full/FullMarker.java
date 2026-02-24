package fixture.full;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({
	ElementType.TYPE,
	ElementType.METHOD,
	ElementType.FIELD,
	ElementType.PARAMETER,
	ElementType.CONSTRUCTOR
})
@Retention(RetentionPolicy.RUNTIME)
public @interface FullMarker {
	String value() default "marker";

	Class<?> target() default Object.class;

	int[] codes() default {1, 2, 3};

	Status status() default Status.NEW;

	Detail detail() default @Detail("single");

	Detail[] details() default {@Detail("left"), @Detail("right")};

	@interface Detail {
		String value() default "detail";
	}
}
