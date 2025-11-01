package io.github.alien.roseau.api.analysis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class CachingApiAnalyzer implements ApiAnalyzer {
	private final Cache<String, Map<String, MethodDecl>> methodsCache =
		CacheBuilder.newBuilder()
			.maximumSize(1_000L)
			.build();
	private final Cache<String, Map<String, FieldDecl>> fieldsCache =
		CacheBuilder.newBuilder()
			.maximumSize(1_000L)
			.build();

	@Override
	public Map<String, MethodDecl> getExportedMethodsByErasure(TypeDecl type) {
		try {
			return methodsCache.get(type.getQualifiedName(), () -> ApiAnalyzer.super.getExportedMethodsByErasure(type));
		} catch (ExecutionException ignored) {
			return Map.of();
		}
	}

	@Override
	public Map<String, FieldDecl> getExportedFieldsByName(TypeDecl type) {
		try {
			return fieldsCache.get(type.getQualifiedName(), () -> ApiAnalyzer.super.getExportedFieldsByName(type));
		} catch (ExecutionException ignored) {
			return Map.of();
		}
	}
}
