package com.github.maracas.roseau.model;

import spoon.reflect.declaration.CtRecord;
import spoon.reflect.declaration.CtType;

public enum TypeKind {
	CLASS, INTERFACE, ENUM, RECORD, ANNOTATION;

	public static TypeKind of(CtType<?> type) {
		if (type.isClass())
			return CLASS;
		if (type.isInterface())
			return INTERFACE;
		if (type.isEnum())
			return ENUM;
		if (type.isAnnotationType())
			return ANNOTATION;
		if (type instanceof CtRecord)
			return RECORD;
		throw new IllegalArgumentException("Unknown type kind " + type);
	}
}
