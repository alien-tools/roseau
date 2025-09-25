package io.github.alien.roseau.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface Client {
	String value();
}
