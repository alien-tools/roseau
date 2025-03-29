package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.github.alien.roseau.api.model.reference.CachingTypeReferenceFactory;
import io.github.alien.roseau.api.resolution.CachingTypeResolver;
import io.github.alien.roseau.api.resolution.SpoonTypeProvider;
import io.github.alien.roseau.api.resolution.TypeProvider;
import io.github.alien.roseau.api.resolution.TypeResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Holds a set of {@link Symbol} extracted from a library and provides convenience methods to access type declarations.
 * All types are immutable and can be serialized/unserialized from/to JSON. To enable type resolution, library types
 * contain
 * <strong>all</strong> the types declared in a library, including non-exported ones. {@link LibraryTypes} instances
 * have limited analysis capabilities and must be transformed into {@link API} to enable type resolution and most
 * analyses ({@link #toAPI(TypeResolver)}).
 */
public final class LibraryTypes implements TypeProvider {
	/**
	 * An immutable map that stores all types within the library, including both exported and non-exported
	 * {@link TypeDecl} instances. Allows for efficient lookup of type declarations by their qualified names.
	 */
	private final ImmutableMap<String, TypeDecl> allTypes;

	private static final Logger LOGGER = LogManager.getLogger(LibraryTypes.class);

	/**
	 * Initializes from the given list of {@link TypeDecl}.
	 *
	 * @param types Initial set of {@link TypeDecl} instances inferred from the library, exported or not
	 */
	public LibraryTypes(@JsonProperty("allTypes") List<TypeDecl> types) {
		Preconditions.checkNotNull(types);
		this.allTypes = types.stream()
			.collect(ImmutableMap.toImmutableMap(
				Symbol::getQualifiedName,
				Function.identity(),
				(fqn, duplicate) -> { throw new IllegalArgumentException("Duplicated types " + fqn); }
			));
	}

	/**
	 * Creates a new resolved API using the given resolver.
	 *
	 * @param resolver the resolver used for type resolution
	 * @return the new resolved API
	 */
	public API toAPI(TypeResolver resolver) {
		return new API(this, resolver);
	}

	/**
	 * Convenience method to create a resolved API using a default resolver.
	 *
	 * @return the new resolved API
	 */
	public API toAPI() {
		// FIXME: this is just temporary
		TypeProvider reflectiveTypeProvider = new SpoonTypeProvider(new CachingTypeReferenceFactory(), List.of());
		return new API(this, new CachingTypeResolver(List.of(this, reflectiveTypeProvider)));
	}

	/**
	 * Returns <strong>all</strong> types contained in the API, including those that are *not* exported.
	 *
	 * @return The list of <strong>all</strong> {@link TypeDecl}
	 */
	@JsonProperty("allTypes")
	public List<TypeDecl> getAllTypes() {
		return allTypes.values().stream().toList();
	}

	/**
	 * Returns the type, <strong>exported or not</strong>, with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @param type the expected type kind
	 * @param <T> the expected type kind
	 * @return An {@link Optional} indicating whether the type was found
	 */
	@Override
	public <T extends TypeDecl> Optional<T> findType(String qualifiedName, Class<T> type) {
		Optional<TypeDecl> resolved = Optional.ofNullable(allTypes.get(qualifiedName));

		if (resolved.isPresent() && !type.isInstance(resolved.get())) {
				LOGGER.warn("Type {} is not of expected type {}", qualifiedName, type);
				return Optional.empty();
		}

		return resolved.map(type::cast);
	}

	/**
	 * Serializes the API as Json to the specified file.
	 *
	 * @param jsonFile the {@link Path} to write to
	 * @throws IOException if serialization fails
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
	 * Parses the given Json file as a new API
	 *
	 * @param jsonFile the {@link Path} to read Json from
	 * @return the API generated from the Json file
	 * @throws IOException If the file cannot be parsed
	 */
	public static LibraryTypes fromJson(Path jsonFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		// For Optional<>
		mapper.registerModule(new Jdk8Module());
		// For @JsonCreator
		mapper.registerModule(new ParanamerModule());
		return mapper.readValue(jsonFile.toFile(), LibraryTypes.class);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LibraryTypes api = (LibraryTypes) o;
		return Objects.equals(allTypes, api.allTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(allTypes);
	}
}
