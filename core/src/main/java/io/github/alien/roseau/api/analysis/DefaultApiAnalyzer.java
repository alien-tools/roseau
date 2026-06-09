package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.LibraryTypes;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class DefaultApiAnalyzer implements ApiAnalyzer {
	private final Cache<String, Map<String, MethodDecl>> methodsCache =
		CacheBuilder.newBuilder()
			.maximumSize(2_000L)
			.build();
	private final Cache<String, Map<String, FieldDecl>> fieldsCache =
		CacheBuilder.newBuilder()
			.maximumSize(2_000L)
			.build();

	private final LibraryTypes libraryTypes;
	private final TypeResolver resolver;

	public DefaultApiAnalyzer(LibraryTypes libraryTypes, TypeResolver resolver) {
		this.libraryTypes = Preconditions.checkNotNull(libraryTypes);
		this.resolver = Preconditions.checkNotNull(resolver);
	}

	@Override
	public TypeResolver resolver() {
		return resolver;
	}

	@Override
	public boolean isExported(TypeDecl type) {
		return isModuleExported(type) && ApiAnalyzer.super.isExported(type);
	}

	private boolean isModuleExported(TypeDecl type) {
		return libraryTypes.getModule().isExporting(type.getPackageName());
	}

	@Override
	public Map<String, MethodDecl> getExportedMethodsByErasure(TypeDecl type) {
		try {
			return methodsCache.get(type.getQualifiedName(), () -> ApiAnalyzer.super.getExportedMethodsByErasure(type));
		} catch (ExecutionException _) {
			return Map.of();
		}
	}

	@Override
	public Map<String, FieldDecl> getExportedFieldsByName(TypeDecl type) {
		try {
			return fieldsCache.get(type.getQualifiedName(), () -> ApiAnalyzer.super.getExportedFieldsByName(type));
		} catch (ExecutionException _) {
			return Map.of();
		}
	}
}
