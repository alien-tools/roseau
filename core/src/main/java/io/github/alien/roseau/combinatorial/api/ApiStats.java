package io.github.alien.roseau.combinatorial.api;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ApiStats {
	private static final Logger LOGGER = LogManager.getLogger();

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

		LOGGER.info("--------------------------------");
		LOGGER.info("---------- API stats -----------");
		LOGGER.info("--------------------------------");
		LOGGER.info(api.getAllTypes().count() + " types");
		LOGGER.info("--------------------------------");
		LOGGER.info(classesCount + " classes");
		LOGGER.info(interfacesCount + " interfaces");
		LOGGER.info(enumsCount + " enums");
		LOGGER.info(recordsCount + " records");
		LOGGER.info("--------------------------------");
		LOGGER.info(constructorsCount + " constructors");
		LOGGER.info(methodsCount + " methods");
		LOGGER.info(fieldsCount + " fields");
		LOGGER.info(enumValuesCount + " enum values");
		LOGGER.info("--------------------------------\n");
	}

	private static void countEntities(TypeDecl type) {
		methodsCount += (int) type.getDeclaredMethods().stream().filter(TypeMemberDecl::isExported).count();
		fieldsCount += (int) type.getDeclaredFields().stream().filter(TypeMemberDecl::isExported).count();

		if (type instanceof EnumDecl enumDecl) {
			enumValuesCount += enumDecl.getValues().size();
		}

		if (type instanceof ClassDecl classDecl) {
			constructorsCount += (int) classDecl.getDeclaredConstructors().stream().filter(TypeMemberDecl::isExported).count();
		}
	}
}
