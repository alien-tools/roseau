package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import io.github.alien.roseau.Library;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.resolution.TypeProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Holds a set of {@link Symbol} extracted from a library and provides convenience methods to access type declarations.
 * All types are immutable and can be serialized/unserialized from/to JSON. To enable type resolution, library types
 * contain <strong>all</strong> the types declared in a library, including non-exported ones. {@link LibraryTypes}
 * instances are a pure extracted snapshot; type resolution and semantic analysis are added separately by
 * {@link API}.
 */
public final class LibraryTypes implements TypeProvider {
	/**
	 * The analyzed library
	 */
	private final Library library;

	/**
	 * The module corresponding to the library.
	 */
	private final ModuleDecl module;

	/**
	 * An immutable map that stores all types within the library, including both exported and non-exported
	 * {@link TypeDecl} instances. Allows for efficient lookup of type declarations by their qualified names.
	 */
	private final Map<String, TypeDecl> allTypes;

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Logger LOGGER = LogManager.getLogger(LibraryTypes.class);

	static {
		MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		MAPPER.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
		// For Optional<>
		MAPPER.registerModule(new Jdk8Module());
		// For @JsonCreator
		MAPPER.registerModule(new ParanamerModule());
	}

	/**
	 * Initializes from the given list of {@link TypeDecl} and {@link ModuleDecl}.
	 *
	 * @param library The analyzed library
	 * @param module  The module corresponding to the library
	 * @param types   Initial set of {@link TypeDecl} instances inferred from the library, exported or not
	 */
	@JsonCreator
	public LibraryTypes(Library library, ModuleDecl module, @JsonProperty("allTypes") Set<TypeDecl> types) {
		Preconditions.checkNotNull(library);
		Preconditions.checkNotNull(module);
		Preconditions.checkNotNull(types);
		this.library = library;
		this.module = module;
		allTypes = types.stream()
			.collect(ImmutableSortedMap.toImmutableSortedMap(
				Comparator.naturalOrder(),
				Symbol::getQualifiedName,
				Function.identity(),
				(fqn, _) -> {
					throw new RoseauException("Duplicated type in %s: %s".formatted(library, fqn));
				}
			));
	}

	/**
	 * Initializes from the given list of {@link TypeDecl}.
	 *
	 * @param library The analyzed library
	 * @param types   Initial set of {@link TypeDecl} instances inferred from the library, exported or not
	 */
	public LibraryTypes(Library library, Set<TypeDecl> types) {
		this(library, ModuleDecl.UNNAMED_MODULE, types);
	}

	/**
	 * The analyzed library.
	 *
	 * @return the library
	 */
	@JsonProperty("library")
	public Library getLibrary() {
		return library;
	}

	/**
	 * The module corresponding to the library.
	 *
	 * @return the module
	 */
	@JsonProperty("module")
	public ModuleDecl getModule() {
		return module;
	}

	/**
	 * Returns <strong>all</strong> types contained in the API, including those that are *not* exported.
	 *
	 * @return The list of <strong>all</strong> {@link TypeDecl}
	 */
	@JsonProperty("allTypes")
	public Collection<TypeDecl> getAllTypes() {
		return allTypes.values();
	}

	/**
	 * Returns the type, <strong>exported or not</strong>, with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @param type          the expected type kind
	 * @param <T>           the expected type kind
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
		MAPPER.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
	}

	/**
	 * Parses the given Json file as extracted library types.
	 *
	 * @param jsonFile the {@link Path} to read Json from
	 * @return the library types generated from the Json file
	 * @throws IOException If the file cannot be parsed
	 */
	public static LibraryTypes fromJson(Path jsonFile) throws IOException {
		return MAPPER.readValue(jsonFile.toFile(), LibraryTypes.class);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof LibraryTypes other
			&& Objects.equals(library, other.library)
			&& Objects.equals(module, other.module)
			&& Objects.equals(allTypes, other.allTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(library, module, allTypes);
	}
}
