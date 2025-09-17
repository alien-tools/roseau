package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

abstract class SymbolBuilder {
	public String qualifiedName;
	public AccessModifier visibility;
	public EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
	public List<Annotation> annotations = new ArrayList<>();
	public SourceLocation location = SourceLocation.NO_LOCATION;

	protected void mutateWithDecl(Symbol symbol) {
		qualifiedName = symbol.getQualifiedName();
		visibility = symbol.getVisibility();
		modifiers = symbol.getModifiers().isEmpty()
				? EnumSet.noneOf(Modifier.class)
				: EnumSet.copyOf(symbol.getModifiers());
		annotations = new ArrayList<>(symbol.getAnnotations());
		location = symbol.getLocation();
	}
}
