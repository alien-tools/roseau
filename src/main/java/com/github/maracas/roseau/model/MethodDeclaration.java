package com.github.maracas.roseau.model;

import java.util.List;

public class MethodDeclaration {
    public String name;

    public TypeDeclaration type;
    public AccessModifier visibility;
    public String returnType;
    public List<String> returnTypeReferencedType;
    public List<String> parametersTypes;
    public List<List<String>> parametersReferencedTypes;     // Meh idk, maybe change the way you deal with generics ...
    public List<String> formalTypeParameters;
    public List<NonAccessModifiers> Modifiers;
    public Signature signature;
    public List<String> exceptions;
    public List<Boolean> parametersVarargsCheck;
    public boolean isDefault;


    public MethodDeclaration(String name, TypeDeclaration type, AccessModifier visibility, String returnType, List<String> returnTypeReferencedType, List<String> parametersTypes, List<List<String>> parametersReferencedTypes, List<String> formalTypeParameters, List<NonAccessModifiers> Modifiers, Signature signature, List<String> exceptions, List<Boolean> parametersVarargsCheck, boolean isDefault) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.returnType = returnType;
        this.returnTypeReferencedType = returnTypeReferencedType;
        this.parametersTypes = parametersTypes;
        this.parametersReferencedTypes = parametersReferencedTypes;
        this.formalTypeParameters = formalTypeParameters;
        this.Modifiers = Modifiers;
        this.signature = signature;
        this.exceptions = exceptions;
        this.parametersVarargsCheck = parametersVarargsCheck;
        this.isDefault = isDefault;


    }

    public String getName() {
        return name;
    }

    public TypeDeclaration getType() { return type; }

    public AccessModifier getVisibility() {
        return visibility;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getReturnTypeReferencedTypes() { return returnTypeReferencedType; }

    public List<String> getParametersTypes() {
        return parametersTypes;
    }

    public List<List<String>> getParametersReferencedTypes() { return parametersReferencedTypes; }

    public List<String> getFormalTypeParameters() {
        return formalTypeParameters;
    }

    public List<NonAccessModifiers> getModifiers() { return Modifiers; }

    public Signature getSignature() { return signature; }

    public List<String> getExceptions() { return exceptions; }

    public List<Boolean> getParametersVarargsCheck() { return parametersVarargsCheck; }

    public boolean isDefault() {return isDefault; }

    public void printMethod() {
        System.out.println("Method: " + visibility + " " + returnType + " " + name);
    }
}
