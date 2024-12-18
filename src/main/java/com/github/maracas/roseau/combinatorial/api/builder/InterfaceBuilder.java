package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.InterfaceDecl;

public class InterfaceBuilder extends TypeDeclBuilder {
    public InterfaceDecl make() {
        return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location,
                implementedInterfaces, formalTypeParameters, fields, methods, enclosingType);
    }
}
