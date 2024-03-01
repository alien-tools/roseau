package com.github.maracas.roseau.api.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A representation of all types contained in a given library, including those
 * that are exported ({@link #getExportedTypes()}}. The entire API is weakly immutable
 * and can be serialized/deserialized from Json.
 */
public final class API {
	private final Map<String, TypeDecl> allTypes;
	@JsonIgnore
	private final SpoonAPIFactory factory;

	/**
	 * Initializes an API from the given list of {@link TypeDecl} and the given {@link SpoonAPIFactory}.
	 * Every {@link com.github.maracas.roseau.api.model.reference.TypeReference} in the given list of types
	 * is visited to resolve within-library types and assign the {@code factory} for later type resolutions.
	 *
	 * @param types   Initial set of {@link TypeDecl} instances inferred from the library, exported or not
	 * @param factory Passed around to every type reference for later {@link TypeDecl} inference and resolution
	 */
	public API(@JsonProperty("allTypes") List<TypeDecl> types, @JacksonInject SpoonAPIFactory factory) {
		this.allTypes = Objects.requireNonNull(types).stream()
			.collect(Collectors.toMap(
				Symbol::getQualifiedName,
				Function.identity()
			));
		this.factory = Objects.requireNonNull(factory);

		// Whenever we create an API instance, we need to make sure to resolve within-library types and to pass
		// the factory around to lazily resolve type references later
		new AbstractAPIVisitor() {
			@Override
			public <U extends TypeDecl> Visit typeReference(TypeReference<U> it) {
				return () -> {
					it.setFactory(factory);
					if (allTypes.containsKey(it.getQualifiedName()))
						it.setResolvedApiType((U) allTypes.get(it.getQualifiedName()));
				};
			}
		}.$(this).visit();
	}

	/**
	 * Returns the {@link TypeDecl} that are exported in the API.
	 *
	 * @return The list of exported {@link TypeDecl}
	 */
	public List<TypeDecl> getExportedTypes() {
		return getAllTypes().stream()
			.filter(Symbol::isExported)
			.toList();
	}

	/**
	 * Finds an exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		TypeDecl find = allTypes.get(qualifiedName);

		return find != null && find.isExported() ? Optional.of(find) : Optional.empty();
	}

	/**
	 * Returns the {@link ClassDecl} that are exported in the API.
	 *
	 * @return The list of exported {@link ClassDecl}
	 */
	public List<ClassDecl> getExportedClasses() {
		return getExportedTypes().stream()
			.filter(ClassDecl.class::isInstance)
			.map(ClassDecl.class::cast)
			.toList();
	}

	/**
	 * Returns the {@link InterfaceDecl} that are exported in the API.
	 *
	 * @return The list of exported {@link InterfaceDecl}
	 */
	public List<InterfaceDecl> getExportedInterfaces() {
		return getExportedTypes().stream()
			.filter(InterfaceDecl.class::isInstance)
			.map(InterfaceDecl.class::cast)
			.toList();
	}

	/**
	 * Returns *all* types in the library, including those that are *not* exported.
	 * In most cases, you should probably use {@link #getExportedTypes()} instead.
	 *
	 * @return The list of *all* {@link TypeDecl}
	 * @see    #getExportedTypes() 
	 */
	@JsonProperty("allTypes")
	public List<TypeDecl> getAllTypes() {
		return allTypes.values().stream().toList();
	}

	/**
	 * Finds a type, *exported or not*, with the given qualified name.
	 *
	 * @param  qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 * @see    #findExportedType(String) 
	 */
	public Optional<TypeDecl> findType(String qualifiedName) {
		return Optional.ofNullable(allTypes.get(qualifiedName));
	}

	/**
	 * Retrieves the {@link SpoonAPIFactory} associated with this API.
	 *
	 * @return The SpoonAPIFactory instance.
	 */
	public SpoonAPIFactory getFactory() {
		return factory;
	}

	/**
	 * Serializes the API as Json to the specified file
	 *
	 * @param  jsonFile    The {@link Path} to write to
	 * @throws IOException If the file cannot be written
	 */
	public void writeJson(Path jsonFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.ALL,     JsonAutoDetect.Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD,   JsonAutoDetect.Visibility.ANY);
		mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
		mapper.registerModule(new Jdk8Module());
		mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	/**
	 * Parses the given Json file as an API
	 *
	 * @param  jsonFile The {@link Path} to read Json from
	 * @param  factory  The {@link SpoonAPIFactory} that should be injected in the parsed objects
	 * @return The API generated from the Json file
	 * @throws IOException If the file cannot be parsed
	 */
	public static API fromJson(Path jsonFile, SpoonAPIFactory factory) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module()); // For Optional<>
		mapper.registerModule(new ParanamerModule()); // For @JsonCreator
		mapper.setInjectableValues(new InjectableValues.Std().addValue(SpoonAPIFactory.class, factory));
		return mapper.readValue(jsonFile.toFile(), API.class);
	}

	@Override
	public String toString() {
		return getExportedTypes().stream()
			.map(TypeDecl::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		API api = (API) o;
		return Objects.equals(allTypes, api.allTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(allTypes);
	}
}
