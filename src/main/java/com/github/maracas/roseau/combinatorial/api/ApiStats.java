package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.model.*;

final class ApiStats {
    private static int classesCount;
    private static int interfacesCount;
    private static int enumsCount;
    private static int recordsCount;
    private static int methodsCount;
    private static int fieldsCount;

    static void display(API api) {
        classesCount = 0;
        interfacesCount = 0;
        enumsCount = 0;
        recordsCount = 0;
        methodsCount = 0;
        fieldsCount = 0;

        for (TypeDecl type : api.getAllTypes().toList()) {
            switch (type) {
                case EnumDecl enumDecl:
                    enumsCount++;
                    countMethodsAndFields(enumDecl);
                    break;
                case RecordDecl recordDecl:
                    recordsCount++;
                    countMethodsAndFields(recordDecl);
                    break;
                case ClassDecl classDecl:
                    classesCount++;
                    countMethodsAndFields(classDecl);
                    break;
                case InterfaceDecl interfaceDecl:
                    interfacesCount++;
                    countMethodsAndFields(interfaceDecl);
                    break;
                case AnnotationDecl ignored:
                    break;
            }
        }

        System.out.println("--------------------------------");
        System.out.println("---------- API stats -----------");
        System.out.println("--------------------------------");
        System.out.println(api.getAllTypes().count() + " types");
        System.out.println("--------------------------------");
        System.out.println(classesCount + " classes");
        System.out.println(interfacesCount + " interfaces");
        System.out.println(enumsCount + " enums");
        System.out.println(recordsCount + " records");
        System.out.println("--------------------------------");
        System.out.println(methodsCount + " methods");
        System.out.println(fieldsCount + " fields");
        System.out.println("--------------------------------");
    }

    private static void countMethodsAndFields(TypeDecl type) {
        methodsCount += type.getDeclaredMethods().size();
        fieldsCount += type.getDeclaredFields().size();
    }
}
