package com.github.maracas.roseau.model;


import java.io.File;
import java.util.List;

/**
 * Represents a field declaration in a Java type.
 * This class contains information about the field's name, data type, modifiers, and more.
 */

public class FieldDeclaration {
    /** The simple name of the field. */
    public String name;

    /** The type containing the field. */
    public TypeDeclaration type;

    /** The visibility of the field. */
    public AccessModifier visibility;

    /** The data type of the field (e.g., int, double, class types, interface types). */
    public String dataType;

    /** List of non-access modifiers applied to the field. */
    public List<NonAccessModifiers> Modifiers;

    /** List of types referenced by the field. */
    public List<String> referencedTypes;

    /** The exact position of the field declaration */
    public String position;




    public FieldDeclaration(String name, TypeDeclaration type, AccessModifier visibility, String dataType, List<NonAccessModifiers> Modifiers, List<String> referencedTypes, String position) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.dataType = dataType;
        this.Modifiers = Modifiers;
        this.referencedTypes = referencedTypes;
        this.position = position;

    }

    /**
     * Retrieves the simple name of the field.
     * @return Field's simple name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the TypeDeclaration containing the field.
     * @return Type containing the field
     */
    public TypeDeclaration getType() {
        return type;
    }

    /**
     * Retrieves the visibility of the field.
     * @return Field's visibility
     */
    public AccessModifier getVisibility() {
        return visibility;
    }

    /**
     * Retrieves the data type of the field (e.g., int, double, class types, interface types).
     * @return Field's data type
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Retrieves the list of non-access modifiers applied to the field.
     * @return Field's non-access modifiers
     */
    public List<NonAccessModifiers> getModifiers() {
        return Modifiers;
    }

    /**
     * Retrieves the list of types referenced by this field.
     * @return List of types referenced by the field
     */
    public List<String> getReferencedTypes() {
        return referencedTypes;
    }

    /**
     * Retrieves the position of the field declaration.
     * @return Field's position.
     */
    public String getPosition() {
        return position;
    }





    public void printField() {
        System.out.println("Field: " + visibility + " " + dataType + " " + name);
    }
}