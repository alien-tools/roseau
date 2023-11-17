package com.github.maracas.roseau.api.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the API of a library containing all the types, each of which may have methods, fields, constructors, and more information about the type.
 * This class encapsulates a list of {@link TypeDecl} instances, each representing distinct types identified by their respective qualified names.
 *
 * @param types The list of TypeDeclarations representing all the types in the library's API.
 */
public record API(List<TypeDecl> types) {
	public List<TypeDecl> getAllTypes() {
		return types;
	}

	public List<TypeDecl> getExportedTypes() {
		return types.stream()
			.filter(Symbol::isExported)
			.toList();
	}

	public List<ClassDecl> getExportedClasses() {
		return types.stream()
			.filter(ClassDecl.class::isInstance)
			.map(ClassDecl.class::cast)
			.toList();
	}

	public List<InterfaceDecl> getExportedInterfaces() {
		return types.stream()
			.filter(InterfaceDecl.class::isInstance)
			.map(InterfaceDecl.class::cast)
			.toList();
	}

	public Optional<TypeDecl> getExportedType(String qualifiedName) {
		return getExportedTypes().stream()
			.filter(t -> Objects.equals(qualifiedName, t.qualifiedName))
			.findFirst();
	}

	public void writeJson(Path jsonFile) throws IOException {
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (TypeDecl typeDeclaration : types) {
			builder.append(typeDeclaration).append("\n");
			builder.append("    =========================\n\n");
		}

		return builder.toString();
	}
}
