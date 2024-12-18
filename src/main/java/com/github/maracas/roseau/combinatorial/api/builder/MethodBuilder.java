package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.MethodDecl;

public class MethodBuilder extends ExecutableBuilder {
    public MethodDecl make() {
        return new MethodDecl(qualifiedName, visibility, modifiers, annotations, location,
                containingType, type, parameters, formalTypeParameters, thrownExceptions);
    }
}
