package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.resolution.TypeResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingAPIAnalyzer extends APIAnalyzer {
	private final Map<String, List<MethodDecl>> methodsCache = new ConcurrentHashMap<>();
	private final Map<String, List<FieldDecl>> fieldsCache = new ConcurrentHashMap<>();

	public CachingAPIAnalyzer(TypeResolver resolver) {
		super(resolver);
	}

	@Override
	public List<MethodDecl> getAllMethods(TypeDecl type) {
		return methodsCache.computeIfAbsent(type.getQualifiedName(), t -> super.getAllMethods(type));
	}

	@Override
	public List<FieldDecl> getAllFields(TypeDecl type) {
		return fieldsCache.computeIfAbsent(type.getQualifiedName(), t -> super.getAllFields(type));
	}
}
