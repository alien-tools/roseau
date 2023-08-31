package com.github.maracas.roseau.model;


import java.util.List;

/**
 * Represents a constructor declaration within a Java type.
 * This class contains information about the constructor's name, modifiers, parameters,
 * return data types (for constructor-like methods), and more.
 */
public class ConstructorDeclaration {
    /** The simple name of the constructor. */
    public String name;

    /** The type containing the constructor. */
    public TypeDeclaration type;

    /** The visibility of the constructor. */
    public AccessModifier visibility;

    /** The return data type of the constructor. */
    public String returnType;

    /** List of referenced types in the constructor's return type. */
    public List<String> returnTypeReferencedType;

    /** List of the constructor's parameter data types. */
    public List<String> parametersTypes;

    /** List of referenced types for each constructor parameter. */
    public List<List<String>> parametersReferencedTypes;

    /** List of the constructor's formal type parameters. */
    public List<String> formalTypeParameters;

    /** List of non-access modifiers applied to the constructor. */
    public List<NonAccessModifiers> Modifiers;

    /** The constructor's signature. */
    public Signature signature;

    /** List of exceptions thrown by the constructor. */
    public List<String> exceptions;

    /** The exact position of the constructor declaration */
    public String position;



    public ConstructorDeclaration(String name, TypeDeclaration type, AccessModifier visibility, String returnType, List<String> returnTypeReferencedType, List<String> parametersTypes, List<List<String>> parametersReferencedTypes, List<String> formalTypeParameters, List<NonAccessModifiers> Modifiers, Signature signature, List<String> exceptions, String position) {
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
        this.position = position;
    }

    /**
     * Retrieves the simple name of the constructor.
     * @return Constructor's simple name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the TypeDeclaration containing the constructor.
     * @return Type containing the constructor
     */
    public TypeDeclaration getType() {
        return type;
    }

    /**
     * Retrieves the visibility of the constructor.
     * @return Constructor's visibility
     */
    public AccessModifier getVisibility() {
        return visibility;
    }

    /**
     * Retrieves the return data type of the constructor.
     * @return Constructor's return data type
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * Retrieves the list of referenced types in the return type of the constructor.
     * @return List of referenced types in the return type
     */
    public List<String> getReturnTypeReferencedTypes() {
        return returnTypeReferencedType;
    }

    /**
     * Retrieves the list of parameter data types of the constructor.
     * @return List of parameter data types
     */
    public List<String> getParametersTypes() {
        return parametersTypes;
    }

    /**
     * Retrieves the list of referenced types for each parameter of the constructor.
     * @return Lists of referenced types for parameters
     */
    public List<List<String>> getParametersReferencedTypes() {
        return parametersReferencedTypes;
    }

    /**
     * Retrieves the constructor's formal type parameters.
     * @return List of formal type parameters
     */
    public List<String> getFormalTypeParameters() {
        return formalTypeParameters;
    }

    /**
     * Retrieves the list of non-access modifiers applied to the constructor.
     * @return List of non-access modifiers
     */
    public List<NonAccessModifiers> getModifiers() {
        return Modifiers;
    }

    /**
     * Retrieves the signature of the constructor.
     * @return The constructor's signature
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * Retrieves the list of exceptions thrown by the constructor.
     * @return List of exceptions thrown by the constructor
     */
    public List<String> getExceptions() {
        return exceptions;
    }

    /**
     * Retrieves the position of the constructor declaration.
     * @return Constructor's position.
     */
    public String getPosition() {
        return position;
    }

    public void printConstructor() {
        System.out.println("Constructor: " + visibility + " " + returnType + " " + name);
    }
}
