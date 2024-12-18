package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeMember;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

abstract class TypeMemberBuilder extends SymbolBuilder implements Builder<TypeMember> {
    public TypeReference<TypeDecl> containingType;
    public ITypeReference type;
}
