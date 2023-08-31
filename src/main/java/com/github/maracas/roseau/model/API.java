package com.github.maracas.roseau.model;

import java.util.List;

/**
 * Represents the API of a library containing all the types, each of which may have methods, fields, constructors, and more information about the type.
 * This class encapsulates a list of {@link TypeDeclaration} instances, each representing distinct types identified by their respective qualified names.
 */
public class API {
    /** The list of TypeDeclarations representing all the types in the library's API. */
    public List<TypeDeclaration> allTheTypes;

    /**
     * Constructs an API instance with the provided list of TypeDeclarations.
     *
     * @param allTheTypes The list of TypeDeclarations representing all the types in the API
     */
    public API(List<TypeDeclaration> allTheTypes) {
        this.allTheTypes = allTheTypes;
    }

    /**
     * Retrieves the list of TypeDeclarations representing all the types in the API.
     *
     * @return List of all TypeDeclarations within the library
     */
    public List<TypeDeclaration> getAllTheTypes() {
        return allTheTypes;
    }

    /**
     * Sets the list of TypeDeclarations representing all the types in the API.
     *
     * @param allTheTypes The list of all TypeDeclarations to be set
     */
    public void setAllTheTypes(List<TypeDeclaration> allTheTypes) {
        this.allTheTypes = allTheTypes;
    }
}
