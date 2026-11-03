package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtractorSink {
	private final Set<TypeDecl> types;
	private final Set<ModuleDecl> modules;
	private final Map<String, Set<String>> unresolvedTypes;

	public ExtractorSink(int initialCapacity) {
		this.types = ConcurrentHashMap.newKeySet(initialCapacity);
		this.modules = ConcurrentHashMap.newKeySet(1);
		this.unresolvedTypes = new ConcurrentHashMap<>();
	}

	public void accept(TypeDecl type) {
		types.add(type);
	}

	public void accept(ModuleDecl module) {
		modules.add(module);
	}

	public void acceptUnresolvedTypes(String declaringType, Set<String> types) {
		unresolvedTypes.put(declaringType, types);
	}

	public Set<TypeDecl> getTypes() {
		return types;
	}

	public Set<ModuleDecl> getModules() {
		return modules;
	}

	public Map<String, Set<String>> getUnresolvedTypes() {
		return unresolvedTypes;
	}
}
