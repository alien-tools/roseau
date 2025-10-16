package io.github.alien.roseau.combinatorial.api;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class ApiStats {
	private static final Logger LOGGER = LogManager.getLogger(ApiStats.class);

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

		for (TypeDecl type : api.getLibraryTypes().getAllTypes()) {
			switch (type) {
				case EnumDecl enumDecl:
					enumsCount++;
					countEntities(enumDecl, api);
					break;
				case RecordDecl recordDecl:
					recordsCount++;
					countEntities(recordDecl, api);
					break;
				case ClassDecl classDecl:
					classesCount++;
					countEntities(classDecl, api);
					break;
				case InterfaceDecl interfaceDecl:
					interfacesCount++;
					countEntities(interfaceDecl, api);
					break;
				case AnnotationDecl ignored:
					break;
			}
		}

		LOGGER.info("--------------------------------");
		LOGGER.info("---------- API stats -----------");
		LOGGER.info("--------------------------------");
		LOGGER.info(api.getLibraryTypes().getAllTypes().size() + " types");
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

	private static void countEntities(TypeDecl type, API api) {
		methodsCount += api.getExportedMethods(type).size();
		fieldsCount += api.getExportedFields(type).size();

		if (type instanceof EnumDecl enumDecl) {
			enumValuesCount += enumDecl.getValues().size();
		}

		if (type instanceof ClassDecl classDecl) {
			constructorsCount += classDecl.getDeclaredConstructors().size();
		}
	}
}
