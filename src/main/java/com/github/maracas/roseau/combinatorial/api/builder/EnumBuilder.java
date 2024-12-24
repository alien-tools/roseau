package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.EnumDecl;

import java.util.ArrayList;
import java.util.List;

public final class EnumBuilder extends ClassBuilder {
    public List<String> values = new ArrayList<>();

    public EnumDecl make() {
        return new EnumDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
                fields, methods, enclosingType, constructors, values);
    }
}
