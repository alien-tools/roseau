package com.github.maracas.roseau.combinatorial.api.builder;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;

abstract sealed class ExecutableBuilder extends TypeMemberBuilder permits ConstructorBuilder, MethodBuilder {
    public List<ParameterDecl> parameters = new ArrayList<>();
    public List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
    public List<TypeReference<ClassDecl>> thrownExceptions = new ArrayList<>();

    public void resetParameters() {
        parameters = new ArrayList<>();
    }
}
