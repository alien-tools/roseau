package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

abstract class SymbolBuilder {
	public String qualifiedName;
	public AccessModifier visibility;
	public EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
	public List<Annotation> annotations = new ArrayList<>();
	public SourceLocation location = SourceLocation.NO_LOCATION;

	public static void mutateSymbolBuilderWithSymbol(SymbolBuilder builder, Symbol typeDecl) {
		builder.qualifiedName = typeDecl.getQualifiedName();
		builder.visibility = typeDecl.getVisibility();
		builder.modifiers = typeDecl.getModifiers().isEmpty()
				? EnumSet.noneOf(Modifier.class)
				: EnumSet.copyOf(typeDecl.getModifiers());
		builder.annotations = typeDecl.getAnnotations();
		builder.location = typeDecl.getLocation();
	}
}
