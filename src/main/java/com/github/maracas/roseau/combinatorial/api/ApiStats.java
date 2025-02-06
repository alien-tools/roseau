package com.github.maracas.roseau.combinatorial.api;

import com.github.maracas.roseau.api.model.*;

final class ApiStats {
	private static int constructorsCount;
	private static int methodsCount;
	private static int fieldsCount;
	private static int enumValuesCount;

	static void display(API api) {
		int classesCount = 0;
		int interfacesCount = 0;
		int enumsCount = 0;
		int recordsCount = 0;
		constructorsCount = 0;
		methodsCount = 0;
		fieldsCount = 0;
		enumValuesCount = 0;

		for (TypeDecl type : api.getAllTypes().toList()) {
			switch (type) {
				case EnumDecl enumDecl:
					enumsCount++;
					countEntities(enumDecl);
					break;
				case RecordDecl recordDecl:
					recordsCount++;
					countEntities(recordDecl);
					break;
				case ClassDecl classDecl:
					classesCount++;
					countEntities(classDecl);
					break;
				case InterfaceDecl interfaceDecl:
					interfacesCount++;
					countEntities(interfaceDecl);
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
		System.out.println(constructorsCount + " constructors");
		System.out.println(methodsCount + " methods");
		System.out.println(fieldsCount + " fields");
		System.out.println(enumValuesCount + " enum values");
		System.out.println("--------------------------------");
	}

	private static void countEntities(TypeDecl type) {
		methodsCount += (int) type.getDeclaredMethods().stream().filter(TypeMemberDecl::isExported).count();
		fieldsCount += (int) type.getDeclaredFields().stream().filter(TypeMemberDecl::isExported).count();

		if (type instanceof EnumDecl enumDecl) {
			enumValuesCount += enumDecl.getValues().size();
		}

		if (type instanceof ClassDecl classDecl) {
			constructorsCount += (int) classDecl.getConstructors().stream().filter(TypeMemberDecl::isExported).count();
		}
	}
}
