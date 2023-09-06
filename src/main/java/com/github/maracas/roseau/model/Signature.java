package com.github.maracas.roseau.model;


import java.util.List;

/**
 * Represents a method signature, consisting of a method name and its parameter's data types.
 */
public class Signature {

    /** The simple name of the method. */
    private String Name;

    /** The list of parameter types that the method accepts. */
    private List<String> parameterTypes;

    /**
     * Constructs a Signature object with the specified method name and parameter types.
     *
     * @param methodName    The simple name of the method.
     * @param parameterTypes The list of parameter types that the method accepts.
     */
    public Signature(String methodName, List<String> parameterTypes) {
        this.Name = methodName;
        this.parameterTypes = parameterTypes;
    }

    /**
     * Retrieves the simple name of the method.
     *
     * @return Method's simple name
     */
    public String getName() {
        return Name;
    }

    /**
     * Retrieves the list of parameter types that the method accepts.
     *
     * @return The list of parameter types
     */
    public List<String> getParameterTypes() {
        return parameterTypes;
    }


    /**
     * Generates a string representation of the Signature.
     *
     * @return A formatted string containing the method's name and the list of parameter types.
     */
    @Override
    public String toString() {
        return "Method Name: " + getName() + "\n" +
                "Parameter Types: " + getParameterTypes() + "\n";
    }

}

