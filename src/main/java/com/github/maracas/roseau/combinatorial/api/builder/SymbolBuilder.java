package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.AccessModifier;
import com.github.maracas.roseau.api.model.Annotation;
import com.github.maracas.roseau.api.model.Modifier;
import com.github.maracas.roseau.api.model.SourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

abstract class SymbolBuilder {
    public String qualifiedName;
    public AccessModifier visibility;
    public EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    public List<Annotation> annotations = new ArrayList<>();
    public SourceLocation location = SourceLocation.NO_LOCATION;
}
