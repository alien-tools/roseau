
package com.github.maracas.roseau.model;
import java.util.List;


/**
 * Represents a type declaration in the library, such as a class, an interface, or an enum.
 * This class contains information about the type's kind, name, modifiers, fields, methods, constructors, and more.
 */


public class TypeDeclaration {

    /** The qualified name of the type. */
    public String name;

    /** The visibility of the type. */
    public AccessModifier visibility;

    /** The type of the declaration (e.g., class, interface, enum). */
    public TypeType typeType;

    /** List of fields declared within the type. */
    public List<FieldDeclaration> fields;

    /** List of methods declared within the type. */
    public List<MethodDeclaration> methods;

    /** List of constructors declared within the type. */
    public List<ConstructorDeclaration> constructors;

    /** List of non-access modifiers applied to the type. */
    public List<NonAccessModifiers> Modifiers;

    /** The qualified name of the superclass ("None" if there isn't any). */
    public String superclassName;

    /** The superclass as a type declaration (null if there isn't any). */
    public TypeDeclaration superclass;

    /** The qualified names of the interfaces implemented by the type. */
    public List<String> superinterfacesNames;

    /** List of types referenced by this type. */
    public List<String> referencedTypes;

    /** List of formal type parameters for generic types. */
    public List<String> formalTypeParameters;

    /** A flag indicating whether the type is nested within another type. */
    public boolean nested;

    /** The exact position of the type declaration */
    public String position;




    public TypeDeclaration(String name, AccessModifier visibility, TypeType typeType, List<NonAccessModifiers> Modifiers,String superclassName, List<String> superinterfacesNames, List<String> referencedTypes, List<String> formalTypeParameters, boolean nested, String position) {
        this.name = name;
        this.visibility = visibility;
        this.typeType = typeType;
        this.Modifiers = Modifiers;
        this.superclassName = superclassName;
        this.superinterfacesNames = superinterfacesNames;
        this.referencedTypes = referencedTypes;
        this.formalTypeParameters = formalTypeParameters;
        this.nested = nested;
        this.position = position;

    }


    /**
     * Retrieves the qualified name of the type.
     * @return Type's qualified name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the visibility of the type.
     * @return Type's visibility
     */
    public AccessModifier getVisibility() {
        return visibility;
    }

    /**
     * Retrieves the type of the declaration (e.g., class, interface, enum).
     * @return Type's kind
     */
    public TypeType getTypeType() {
        return typeType;
    }

    /**
     * Retrieves the list of fields declared within the type.
     * @return List of fields declared within the type
     */
    public List<FieldDeclaration> getFields() {
        return fields;
    }

    /**
     * Retrieves the list of methods declared within the type.
     * @return List of methods declared within the type
     */
    public List<MethodDeclaration> getMethods() {
        return methods;
    }

    /**
     * Retrieves the list of constructors declared within the type.
     * @return List of constructors declared within the type
     */
    public List<ConstructorDeclaration> getConstructors() {
        return constructors;
    }

    /**
     * Retrieves the list of non-access modifiers applied to the type.
     * @return Type's non-access modifiers
     */
    public List<NonAccessModifiers> getModifiers() {
        return Modifiers;
    }

    /**
     * Retrieves the qualified name of the type's superclass ("None" if there isn't any).
     * @return Superclass's qualified name
     */
    public String getSuperclassName() {
        return superclassName;
    }

    /**
     * Retrieves the qualified names of the interfaces implemented by the type.
     * @return Qualified names of the interfaces implemented by the type
     */
    public List<String> getSuperinterfacesNames() {
        return superinterfacesNames;
    }

    /**
     * Retrieves the superclass of the type as a typeDeclaration.
     * @return Type's superclass as a typeDeclaration
     */
    public TypeDeclaration getSuperclass() {
        return superclass;
    }

    /**
     * Retrieves the list of types referenced by the type.
     * @return List of referenced types
     */
    public List<String> getReferencedTypes() {
        return referencedTypes;
    }

    /**
     * Retrieves the list of formal type parameters for generic types.
     * @return List of formal type parameters
     */
    public List<String> getFormalTypeParameters() {
        return formalTypeParameters;
    }

    /**
     * Checks if the type is nested within another type.
     * @return True if the type is nested; false otherwise
     */
    public boolean isNested() {
        return nested;
    }

    /**
     * Retrieves the position of the type declaration.
     * @return Type's position.
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the superclass of the type.
     * @param superclass The superclass to be set
     */

    public void setSuperclass(TypeDeclaration superclass) {
        this.superclass = superclass;
    }

    /**
     * Sets the list of fields declared within the type.
     * @param fields List of fields to be set
     */
    public void setFields(List<FieldDeclaration> fields) {
        this.fields = fields;
    }

    /**
     * Sets the list of methods declared within the type.
     * @param methods List of methods to be set
     */
    public void setMethods(List<MethodDeclaration> methods) {
        this.methods = methods;
    }

    /**
     * Sets the list of constructors declared within the type.
     * @param constructors List of constructors to be set
     */
    public void setConstructors(List<ConstructorDeclaration> constructors) {
        this.constructors = constructors;
    }

    public void printType() {
        System.out.println("Type: " + visibility + " " + typeType + " " + name);
    }
}


