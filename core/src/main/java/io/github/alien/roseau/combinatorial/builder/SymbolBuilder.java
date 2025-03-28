package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.Annotation;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

abstract class SymbolBuilder {
	public String qualifiedName;
	public AccessModifier visibility;
	public EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
	public List<Annotation> annotations = new ArrayList<>();
	public SourceLocation location = SourceLocation.NO_LOCATION;

	public static void mutateSymbolBuilderWithSymbol(SymbolBuilder builder, Symbol symbol) {
		builder.qualifiedName = symbol.getQualifiedName();
		builder.visibility = symbol.getVisibility();
		builder.modifiers = symbol.getModifiers().isEmpty()
				? EnumSet.noneOf(Modifier.class)
				: EnumSet.copyOf(symbol.getModifiers());
		builder.annotations = new ArrayList<>(symbol.getAnnotations());
		builder.location = symbol.getLocation();
	}
}
