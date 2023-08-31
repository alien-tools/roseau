package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents a method declaration within a Java type.
 * This class contains information about the method's name, modifiers, parameters / return data types, and more.
 */

public class MethodDeclaration {
    /** The simple name of the method. */
    public String name;

    /** The type containing the method. */
    public TypeDeclaration type;

    /** The visibility of the method. */
    public AccessModifier visibility;

    /** The return data type of the method. */
    public String returnType;

    /** List of referenced types in the method's return type. */
    public List<String> returnTypeReferencedType;

    /** List of the method's parameter data types. */
    public List<String> parametersTypes;

    /** List of referenced types for each parameter. */
    public List<List<String>> parametersReferencedTypes;

    /** List of the method's formal type parameters. */
    public List<String> formalTypeParameters;

    /** List of non-access modifiers applied to the method. */
    public List<NonAccessModifiers> Modifiers;

    /** The method's signature. */
    public Signature signature;

    /** List of exceptions thrown by the method. */
    public List<String> exceptions;

    /** List of boolean values indicating varargs status for each parameter. */
    public List<Boolean> parametersVarargsCheck;

    /** A flag indicating whether the method is a default method. */
    public boolean isDefault;

    /** The exact position of the method declaration */
    public String position;


    public MethodDeclaration(String name, TypeDeclaration type, AccessModifier visibility, String returnType, List<String> returnTypeReferencedType, List<String> parametersTypes, List<List<String>> parametersReferencedTypes, List<String> formalTypeParameters, List<NonAccessModifiers> Modifiers, Signature signature, List<String> exceptions, List<Boolean> parametersVarargsCheck, boolean isDefault, String position) {
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
        this.position = position;


    }

    /**
     * Retrieves the simple name of the method.
     * @return Method's simple name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the TypeDeclaration containing the method.
     * @return Type containing the method
     */
    public TypeDeclaration getType() {
        return type;
    }

    /**
     * Retrieves the visibility of the method.
     * @return  Method's visibility
     */
    public AccessModifier getVisibility() {
        return visibility;
    }

    /**
     * Retrieves the return data type of the method.
     * @return Method's return data type
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * Retrieves the list of referenced types in the return type of the method.
     * @return List of referenced types in the return type
     */
    public List<String> getReturnTypeReferencedTypes() {
        return returnTypeReferencedType;
    }

    /**
     * Retrieves the list of parameter data types of the method.
     * @return List of parameter data types
     */
    public List<String> getParametersTypes() {
        return parametersTypes;
    }

    /**
     * Retrieves the list of referenced types for each parameter of the method.
     * @return Lists of referenced types for parameters
     */
    public List<List<String>> getParametersReferencedTypes() {
        return parametersReferencedTypes;
    }

    /**
     * Retrieves the method's formal type parameters.
     * @return List of formal type parameters
     */
    public List<String> getFormalTypeParameters() {
        return formalTypeParameters;
    }

    /**
     * Retrieves the list of non-access modifiers applied to the method.
     * @return List of non-access modifiers
     */
    public List<NonAccessModifiers> getModifiers() {
        return Modifiers;
    }

    /**
     * Retrieves the signature of the method.
     * @return The method's signature
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * Retrieves the list of exceptions thrown by the method.
     * @return List of exceptions thrown by the method
     */
    public List<String> getExceptions() {
        return exceptions;
    }

    /**
     * Retrieves the list of boolean values indicating varargs status for each parameter.
     * @return List of varargs status for parameters
     */
    public List<Boolean> getParametersVarargsCheck() {
        return parametersVarargsCheck;
    }

    /**
     * Checks if the method is a default method.
     * @return True if the method is a default method, false otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Retrieves the position of the method declaration.
     * @return Method's position.
     */
    public String getPosition() {
        return position;
    }


    public void printMethod() {
        System.out.println("Method: " + visibility + " " + returnType + " " + name);
    }
}
