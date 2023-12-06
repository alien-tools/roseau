package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import com.github.maracas.roseau.api.TypeResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the API of a library containing all the types, each of which may have methods, fields, constructors, and more information about the type.
 * This class encapsulates a list of {@link TypeDecl} instances, each representing distinct types identified by their respective qualified names.
 */
public final class API {
	private final Map<String, TypeDecl> types;

	@JsonCreator
	public API(@JsonProperty("allTypes") List<TypeDecl> types) {
		this.types = types.stream()
			.collect(Collectors.toMap(
				Symbol::getQualifiedName,
				Function.identity()
			));
	}

	public void resolve() {
		// Within-library type resolution
		new TypeResolver(this).$(this).visit();
	}

	public List<TypeDecl> getAllTypes() {
		return types.values().stream().toList();
	}

	@JsonIgnore
	public List<ClassDecl> getAllClasses() {
		return types.values().stream()
			.filter(ClassDecl.class::isInstance)
			.map(ClassDecl.class::cast)
			.toList();
	}

	@JsonIgnore
	public List<TypeDecl> getExportedTypes() {
		return getAllTypes().stream()
			.filter(Symbol::isExported)
			.toList();
	}

	public Optional<TypeDecl> getExportedType(String qualifiedName) {
		return getExportedTypes().stream()
			.filter(t -> Objects.equals(qualifiedName, t.qualifiedName))
			.findFirst();
	}

	public Optional<TypeDecl> findType(String qualifiedName) {
		return Optional.ofNullable(types.get(qualifiedName));
	}

	public Optional<ClassDecl> findClass(String qualifiedName) {
		return getAllClasses().stream()
			.filter(cls -> cls.getQualifiedName().equals(qualifiedName))
			.findFirst();
	}

	public void writeJson(Path jsonFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module());
		mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	public String toJson() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module());
		return mapper.writeValueAsString(this);
	}

	public static API fromJson(Path jsonFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module()); // For Optional<>
		mapper.registerModule(new ParanamerModule()); // For @JsonCreator
		return mapper.readValue(jsonFile.toFile(), API.class);
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		API api = (API) o;
		return Objects.equals(types, api.types);
	}

	@Override
	public int hashCode() {
		return Objects.hash(types);
	}
}
