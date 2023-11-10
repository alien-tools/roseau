package com.github.maracas.roseau.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the API of a library containing all the types, each of which may have methods, fields, constructors, and more information about the type.
 * This class encapsulates a list of {@link TypeDecl} instances, each representing distinct types identified by their respective qualified names.
 *
 * @param types The list of TypeDeclarations representing all the types in the library's API.
 */
public record API(List<TypeDecl> types) {
	public void writeJson(Path jsonFile) throws IOException {
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	/**
	 * Generates a string representation of the library's API.
	 *
	 * @return A formatted string containing all the API elements structured.
	 */
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
