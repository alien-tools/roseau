package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;

public abstract sealed class TypeDeclBuilder extends SymbolBuilder implements Builder<TypeDecl> permits ClassBuilder, InterfaceBuilder {
    public List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
    public List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
    public List<FieldDecl> fields = new ArrayList<>();
    public List<MethodDecl> methods = new ArrayList<>();
    public TypeReference<TypeDecl> enclosingType;
}
