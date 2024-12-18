package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.ConstructorDecl;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;

public class ClassBuilder extends TypeDeclBuilder {
    public TypeReference<ClassDecl> superClass;
    public List<ConstructorDecl> constructors = new ArrayList<>();

    public ClassDecl make() {
        return new ClassDecl(qualifiedName, visibility, modifiers, annotations, location,
                implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, superClass,
                constructors);
    }
}
