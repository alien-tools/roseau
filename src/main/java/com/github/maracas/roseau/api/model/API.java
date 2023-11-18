package com.github.maracas.roseau.api.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the API of a library containing all the types, each of which may have methods, fields, constructors, and more information about the type.
 * This class encapsulates a list of {@link TypeDecl} instances, each representing distinct types identified by their respective qualified names.
 */
public final class API {
	private final Map<String, TypeDecl> types;

	public API(List<TypeDecl> types) {
		this.types = types.stream()
			.collect(Collectors.toMap(
				Symbol::getQualifiedName,
				Function.identity()
			));
	}

	public List<TypeDecl> getAllTypes() {
		return types.values().stream().toList();
	}

	public List<ClassDecl> getAllClasses() {
		return getAllTypes().stream()
			.filter(ClassDecl.class::isInstance)
			.map(ClassDecl.class::cast)
			.toList();
	}

	public List<InterfaceDecl> getAllInterfaces() {
		return getExportedTypes().stream()
			.filter(InterfaceDecl.class::isInstance)
			.map(InterfaceDecl.class::cast)
			.toList();
	}

	public List<TypeDecl> getExportedTypes() {
		return getAllTypes().stream()
			.filter(Symbol::isExported)
			.toList();
	}

	public List<ClassDecl> getExportedClasses() {
		return getAllClasses().stream()
			.filter(Symbol::isExported)
			.toList();
	}

	public List<InterfaceDecl> getExportedInterfaces() {
		return getAllInterfaces().stream()
			.filter(Symbol::isExported)
			.toList();
	}

	public Optional<TypeDecl> getExportedType(String qualifiedName) {
		return getExportedTypes().stream()
			.filter(t -> Objects.equals(qualifiedName, t.qualifiedName))
			.findFirst();
	}

	public Optional<TypeDecl> getType(String qualifiedName) {
		return Optional.ofNullable(types.get(qualifiedName));
	}

	public Optional<ClassDecl> getClass(String qualifiedName) {
		TypeDecl t = types.get(qualifiedName);

		return t instanceof ClassDecl cls
			? Optional.of(cls)
			: Optional.empty();
	}

	public Optional<InterfaceDecl> getInterface(String qualifiedName) {
		TypeDecl t = types.get(qualifiedName);

		return t instanceof InterfaceDecl intf
			? Optional.of(intf)
			: Optional.empty();
	}

	public void writeJson(Path jsonFile) throws IOException {
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (TypeDecl typeDeclaration : types.values()) {
			builder.append(typeDeclaration).append("\n");
			builder.append("    =========================\n\n");
		}

		return builder.toString();
	}
}
