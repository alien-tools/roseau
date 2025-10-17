package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CachingAPIAnalyzer implements APIAnalyzer {
	private final Map<String, Set<MethodDecl>> methodsCache = new ConcurrentHashMap<>();
	private final Map<String, Set<FieldDecl>> fieldsCache = new ConcurrentHashMap<>();

	@Override
	public Set<MethodDecl> getExportedMethods(TypeDecl type) {
		return methodsCache.computeIfAbsent(type.getQualifiedName(), t -> APIAnalyzer.super.getExportedMethods(type));
	}

	@Override
	public Set<FieldDecl> getExportedFields(TypeDecl type) {
		return fieldsCache.computeIfAbsent(type.getQualifiedName(), t -> APIAnalyzer.super.getExportedFields(type));
	}
}
