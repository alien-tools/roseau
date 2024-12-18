package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.FieldDecl;

public class FieldBuilder extends TypeMemberBuilder {
    public FieldDecl make() {
        return new FieldDecl(qualifiedName, visibility, modifiers, annotations, location,
                containingType, type);
    }
}
