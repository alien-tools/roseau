package io.github.alien.roseau.api.analysis;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Set;
import java.util.concurrent.ExecutionException;

public abstract class CachingAPIAnalyzer implements APIAnalyzer {
	private final Cache<String, Set<MethodDecl>> methodsCache =
		CacheBuilder.newBuilder()
			.maximumSize(1_000L)
			.build();
	private final Cache<String, Set<FieldDecl>> fieldsCache =
		CacheBuilder.newBuilder()
			.maximumSize(1_000L)
			.build();

	@Override
	public Set<MethodDecl> getExportedMethods(TypeDecl type) {
		try {
			return methodsCache.get(type.getQualifiedName(), () -> APIAnalyzer.super.getExportedMethods(type));
		} catch (ExecutionException ignored) {
			return Set.of();
		}
	}

	@Override
	public Set<FieldDecl> getExportedFields(TypeDecl type) {
		try {
			return fieldsCache.get(type.getQualifiedName(), () -> APIAnalyzer.super.getExportedFields(type));
		} catch (ExecutionException ignored) {
			return Set.of();
		}
	}
}
