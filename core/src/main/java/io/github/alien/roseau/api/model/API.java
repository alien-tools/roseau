package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import com.google.common.collect.ImmutableMap;
import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.api.visit.APITypeResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An API holds a set of {@link Symbol} and provides convenience methods to access {@link TypeDecl} declarations. All
 * type references within an API should be created using a shared single {@link TypeReferenceFactory}. APIs are weakly
 * immutable (except for lazily-resolved {@link TypeReference}) and can be serialized/unserialized from/to JSON. To
 * enable type resolution, an API holds <strong>all</strong> the types it declares, including non-exported ones
 * ({@link #getExportedTypes()}).
 */
public final class API {
	/**
	 * An immutable map that stores all types within the library, including both exported and non-exported
	 * {@link TypeDecl} instances. Allows for efficient lookup of type declarations by their qualified names.
	 */
	private final ImmutableMap<String, TypeDecl> allTypes;

	/**
	 * A factory for creating {@link TypeReference} instances. Should be unique and shared by all elements in the API.
	 */
	@JsonIgnore
	private final TypeReferenceFactory factory;

	/**
	 * Initializes an API from the given list of {@link TypeDecl}. Every {@link TypeReference} in the given list of types
	 * is visited to set its factory and resolve within-library types.
	 *
	 * @param types   Initial set of {@link TypeDecl} instances inferred from the library, exported or not
	 * @param factory A unique factory to be shared by all symbols and references
	 * @throws NullPointerException if {@code types} or {@code factory} is null
	 */
	public API(@JsonProperty("allTypes") List<TypeDecl> types, @JacksonInject TypeReferenceFactory factory) {
		this.allTypes = Objects.requireNonNull(types).stream()
				.collect(ImmutableMap.toImmutableMap(
						Symbol::getQualifiedName,
						Function.identity()
				));
		this.factory = Objects.requireNonNull(factory);

		// Whenever we create an API instance, we need to make sure to resolve within-library types
		new APITypeResolver(this, new ReflectiveTypeFactory(factory)).resolve();
	}

	/**
	 * Type declaration that are exported by the API.
	 *
	 * @return The list of exported {@link TypeDecl}
	 */
	public Stream<TypeDecl> getExportedTypes() {
		return getAllTypes()
				.filter(Symbol::isExported);
	}

	/**
	 * Returns the class declarations exported by the API.
	 *
	 * @return The list of exported {@link ClassDecl}
	 */
	public Stream<ClassDecl> getExportedClasses() {
		return getExportedTypes()
				.filter(ClassDecl.class::isInstance)
				.map(ClassDecl.class::cast);
	}

	/**
	 * Returns the interface declarations exported by the API.
	 *
	 * @return The list of exported {@link InterfaceDecl}
	 */
	public Stream<InterfaceDecl> getExportedInterfaces() {
		return getExportedTypes()
				.filter(InterfaceDecl.class::isInstance)
				.map(InterfaceDecl.class::cast);
	}

	/**
	 * Returns <strong>all</strong> types declared by the library, including those that are *not* exported. In most cases,
	 * {@link #getExportedTypes()} should be preferred.
	 *
	 * @return The list of <strong>all</strong> {@link TypeDecl}
	 * @see #getExportedTypes()
	 */
	@JsonProperty("allTypes")
	public Stream<TypeDecl> getAllTypes() {
		return allTypes.values().stream();
	}

	/**
	 * Returns the exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		TypeDecl find = allTypes.get(qualifiedName);

		return find != null && find.isExported()
				? Optional.of(find)
				: Optional.empty();
	}

	/**
	 * Returns the type, <strong>exported or not</strong>, with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 * @see #findExportedType(String)
	 */
	public Optional<TypeDecl> findType(String qualifiedName) {
		return Optional.ofNullable(allTypes.get(qualifiedName));
	}

	/**
	 * Serializes the API as Json to the specified file.
	 *
	 * @param jsonFile The {@link Path} to write to
	 * @throws IOException If the file cannot be written
	 */
	public void writeJson(Path jsonFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
		mapper.registerModule(new Jdk8Module());
		mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	/**
	 * Parses the given Json file as an API
	 *
	 * @param jsonFile The {@link Path} to read Json from
	 * @return The API generated from the Json file
	 * @throws IOException If the file cannot be parsed
	 */
	public static API fromJson(Path jsonFile, TypeReferenceFactory factory) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		// For Optional<>
		mapper.registerModule(new Jdk8Module());
		// For @JsonCreator
		mapper.registerModule(new ParanamerModule());
		mapper.setInjectableValues(new InjectableValues.Std().addValue(TypeReferenceFactory.class, factory));
		return mapper.readValue(jsonFile.toFile(), API.class);
	}

	public TypeReferenceFactory getFactory() {
		return factory;
	}

	@Override
	public String toString() {
		return getExportedTypes()
				.map(TypeDecl::toString)
				.collect(Collectors.joining(System.lineSeparator()));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		API api = (API) o;
		return Objects.equals(allTypes, api.allTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(allTypes);
	}

	public API deepCopy(TypeReferenceFactory factory) {
		var reflectiveTypeFactory = new ReflectiveTypeFactory(factory);

		var clonesTypes = getAllTypes()
				.map(t -> (TypeDecl) t.deepCopy(reflectiveTypeFactory))
				.toList();

		return new API(clonesTypes, factory);
	}
}
