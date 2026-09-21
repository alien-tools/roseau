package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultApiAnalyzer implements ApiAnalyzer {
	private final Map<String, Map<String, MethodDecl>> methodsCache = new ConcurrentHashMap<>(2_000);
	private final Map<String, Map<String, FieldDecl>> fieldsCache = new ConcurrentHashMap<>(2_000);

	private final LibraryTypes libraryTypes;
	private final TypeResolver resolver;
	private final SetMultimap<String, TypeDecl> directKnownSubtypes;

	public DefaultApiAnalyzer(LibraryTypes libraryTypes, TypeResolver resolver) {
		this.libraryTypes = Preconditions.checkNotNull(libraryTypes);
		this.resolver = Preconditions.checkNotNull(resolver);
		this.directKnownSubtypes = buildDirectKnownSubtypesBySuperType(libraryTypes);
	}

	@Override
	public LibraryTypes libraryTypes() {
		return libraryTypes;
	}

	@Override
	public TypeResolver resolver() {
		return resolver;
	}

	@Override
	public Set<TypeDecl> getDirectKnownSubtypes(TypeDecl type) {
		return directKnownSubtypes.get(type.getQualifiedName());
	}

	@Override
	public Map<String, MethodDecl> getAllMethodsByErasure(TypeDecl type) {
		return methodsCache.computeIfAbsent(type.getQualifiedName(), _ -> ApiAnalyzer.super.getAllMethodsByErasure(type));
	}

	@Override
	public Map<String, FieldDecl> getExportedFieldsByName(TypeDecl type) {
		return fieldsCache.computeIfAbsent(type.getQualifiedName(), _ -> ApiAnalyzer.super.getExportedFieldsByName(type));
	}

	private static SetMultimap<String, TypeDecl> buildDirectKnownSubtypesBySuperType(LibraryTypes libraryTypes) {
		HashMultimap<String, TypeDecl> subtypes = HashMultimap.create();
		libraryTypes.getAllTypes().forEach(type ->
			PropertiesProvider.directSuperTypeNames(type).forEach(superTypeName -> subtypes.put(superTypeName, type)));
		return ImmutableSetMultimap.copyOf(subtypes);
	}
}
