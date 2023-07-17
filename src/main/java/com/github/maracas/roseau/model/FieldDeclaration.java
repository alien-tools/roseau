package com.github.maracas.roseau.model;


import java.util.List;

public class FieldDeclaration {
    public String name;
    public TypeDeclaration type;
    public AccessModifier visibility;
    public String dataType;
    public List<NonAccessModifiers> Modifiers;
    public List<String> referencedTypes;



    public FieldDeclaration(String name, TypeDeclaration type, AccessModifier visibility, String dataType, List<NonAccessModifiers> Modifiers, List<String> referencedTypes) {
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.dataType = dataType;
        this.Modifiers = Modifiers;
        this.referencedTypes = referencedTypes;
    }

    public String getName() {
        return name;
    }

    public TypeDeclaration getType() { return type; }

    public AccessModifier getVisibility() {
        return visibility;
    }

    public String getDataType() {
        return dataType;
    }

    public List<NonAccessModifiers> getModifiers() { return Modifiers; }

    public List<String> getReferencedTypes() { return referencedTypes; }




    public void printField() {
        System.out.println("Field: " + visibility + " " + dataType + " " + name);
    }
}