package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.Modifier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class InterfaceBuilder extends TypeDeclBuilder {
	public List<String> permittedTypes = new ArrayList<>();

	public InterfaceDecl make() {
		return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
	}

	public static InterfaceBuilder from(InterfaceDecl decl) {
		var builder = new InterfaceBuilder();

		builder.qualifiedName = decl.getQualifiedName();
		builder.visibility = decl.getVisibility();
		builder.modifiers = decl.getModifiers().isEmpty()
				? EnumSet.noneOf(Modifier.class)
				: EnumSet.copyOf(decl.getModifiers());
		builder.annotations = decl.getAnnotations();
		builder.location = decl.getLocation();
		builder.implementedInterfaces = decl.getImplementedInterfaces();
		builder.formalTypeParameters = decl.getFormalTypeParameters();
		builder.fields = decl.getDeclaredFields();
		builder.methods = decl.getDeclaredMethods();
		builder.enclosingType = decl.getEnclosingType().orElse(null);
		builder.permittedTypes = decl.getPermittedTypes();

		return builder;
	}
}
