
package com.github.maracas.roseau.model;
import java.util.List;


/**
 * Represents a type declaration in the library, such as a class, an interface, or an enum.
 * This class extends the {@link ElementDeclaration} class and contains information about the type's kind, fields, methods, constructors, and more.
 */


public class TypeDeclaration extends ElementDeclaration{


    /** The type of the declaration (e.g., class, interface, enum). */
    private TypeType typeType;

    /** List of fields declared within the type. */
    private List<FieldDeclaration> fields;

    /** List of methods declared within the type. */
    private List<MethodDeclaration> methods;

    /** List of constructors declared within the type. */
    private List<ConstructorDeclaration> constructors;


    /** The qualified name of the superclass ("None" if there isn't any). */
    private String superclassName;

    /** The superclass as a type declaration (null if there isn't any). */
    private TypeDeclaration superclass;

    /** The qualified names of the interfaces implemented by the type. */
    private List<String> superinterfacesNames;


    /** List of formal type parameters for generic types. */
    private List<String> formalTypeParameters;

    /** A flag indicating whether the type is nested within another type. */
    private boolean nested;





    public TypeDeclaration(String name, AccessModifier visibility, TypeType typeType, List<NonAccessModifiers> Modifiers,String superclassName, List<String> superinterfacesNames, List<String> referencedTypes, List<String> formalTypeParameters, boolean nested, String position) {
        super(name, visibility, Modifiers, referencedTypes, position);
        this.typeType = typeType;
        this.superclassName = superclassName;
        this.superinterfacesNames = superinterfacesNames;
        this.formalTypeParameters = formalTypeParameters;
        this.nested = nested;
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



    /**
     * Generates a string representation of the TypeDeclaration.
     *
     * @return A formatted string containing the type name, visibility, type kind,
     * nesting status, position, fields, methods and constructors.
     */
    @Override
    public String toString() {
        return "Type Name: " + getName() + "\n" +
                "Visibility: " + getVisibility() + "\n" +
                "Type kind: " + getTypeType() + "\n" +
                "Modifiers: " + getModifiers() + "\n" +
                "Is Nested: " + isNested() + "\n" +
                "Position: " + getPosition() + "\n\n" +
                "Type's fields: " + getFields().toString() + "\n\n" +
                "Type's Methods: " + getMethods().toString() + "\n\n" +
                "Type's Constructors: " + getConstructors().toString() + "\n" ;

    }

}


