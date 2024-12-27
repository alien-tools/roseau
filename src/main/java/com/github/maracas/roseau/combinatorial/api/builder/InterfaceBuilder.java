package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.InterfaceDecl;

import java.util.ArrayList;
import java.util.List;

public class InterfaceBuilder extends TypeDeclBuilder {
    public List<String> permittedTypes = new ArrayList<>();

    public InterfaceDecl make() {
        return new InterfaceDecl(qualifiedName, visibility, modifiers, annotations, location,
                implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, permittedTypes);
    }
}
