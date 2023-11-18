package com.github.maracas.roseau.api.model;

public interface TypeMember {
	boolean isStatic();

	boolean isPublic();

	boolean isProtected();

	boolean isFinal();

	boolean isNative();

	boolean isStrictFp();
}
