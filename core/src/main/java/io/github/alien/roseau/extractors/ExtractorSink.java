package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtractorSink {
	private final Set<TypeDecl> types;
	private final Set<ModuleDecl> modules;

	public ExtractorSink(int initialCapacity) {
		this.types = ConcurrentHashMap.newKeySet(initialCapacity);
		this.modules = ConcurrentHashMap.newKeySet(1);
	}

	public void accept(TypeDecl type) {
		types.add(type);
	}

	public void accept(ModuleDecl module) {
		modules.add(module);
	}

	public Set<TypeDecl> getTypes() {
		return types;
	}

	public Set<ModuleDecl> getModules() {
		return modules;
	}
}
