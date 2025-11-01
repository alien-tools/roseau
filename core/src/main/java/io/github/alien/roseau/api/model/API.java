package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.analysis.CachingApiAnalyzer;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.resolution.TypeResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * An API augments {@link LibraryTypes} with analysis capabilities and symbol export information.
 */
public class API extends CachingApiAnalyzer {
	/**
	 * The types, exported or not, declared in the library.
	 */
	private final LibraryTypes libraryTypes;
	private final TypeResolver typeResolver;
	private final Set<Pattern> namePatterns;

	private static final Logger LOGGER = LogManager.getLogger(API.class);

	public API(LibraryTypes libraryTypes, TypeResolver typeResolver) {
		Preconditions.checkNotNull(libraryTypes);
		Preconditions.checkNotNull(typeResolver);
		this.libraryTypes = libraryTypes;
		this.typeResolver = typeResolver;
		this.namePatterns = libraryTypes.getLibrary().getExclusions().names().stream()
			.map(name -> {
				try {
					return Pattern.compile(name);
				} catch (PatternSyntaxException e) {
					LOGGER.warn("Invalid exclusion pattern {}", name, e);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toUnmodifiableSet());
	}

	public LibraryTypes getLibraryTypes() {
		return libraryTypes;
	}

	@Override
	public TypeResolver resolver() {
		return typeResolver;
	}

	@Override
	public boolean isExported(TypeDecl type) {
		return isModuleExported(type) && super.isExported(type);
	}

	public boolean isModuleExported(TypeDecl type) {
		return libraryTypes.getModule().isExporting(type.getPackageName());
	}

	public boolean isExcluded(Symbol symbol) {
		boolean isAnnotationExcluded = libraryTypes.getLibrary().getExclusions().annotations().stream()
			.anyMatch(ann -> symbol.hasAnnotation(new TypeReference<>(ann.name()), ann.args()));
		boolean isNameExcluded = namePatterns.stream()
			.anyMatch(pattern -> pattern.matcher(symbol.getQualifiedName()).matches());

		return switch (symbol) {
			case TypeDecl type -> isAnnotationExcluded || isNameExcluded ||
				type.getEnclosingType().map(t -> resolver().resolve(t).map(this::isExcluded).orElse(false)).orElse(false);
			case TypeMemberDecl member -> isAnnotationExcluded || isNameExcluded ||
				resolver().resolve(member.getContainingType()).map(this::isExcluded).orElse(false);
		};
	}

	/**
	 * Type declarations that are exported by the API.
	 *
	 * @return The list of exported {@link TypeDecl}
	 */
	public List<TypeDecl> getExportedTypes() {
		return libraryTypes.getAllTypes().stream()
			.filter(this::isExported)
			.toList();
	}

	/**
	 * Returns the exported type in the API with the given qualified name.
	 *
	 * @param qualifiedName The qualified name of the type to find
	 * @return An {@link Optional} indicating whether the type was found
	 */
	public Optional<TypeDecl> findExportedType(String qualifiedName) {
		return libraryTypes.findType(qualifiedName)
			.filter(this::isExported);
	}

	@Override
	public String toString() {
		return getExportedTypes().stream()
			.map(TypeDecl::toString)
			.collect(Collectors.joining(System.lineSeparator()));
	}
}
