package io.github.alien.roseau.extractors;

import io.github.alien.roseau.api.model.ExtractionGap;
import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtractorSink {
	private final Set<TypeDecl> types;
	private final Set<ModuleDecl> modules;
	private final Set<ExtractionGap> gaps;

	public ExtractorSink(int initialCapacity) {
		this.types = ConcurrentHashMap.newKeySet(initialCapacity);
		this.modules = ConcurrentHashMap.newKeySet(1);
		this.gaps = ConcurrentHashMap.newKeySet();
	}

	public void accept(TypeDecl type) {
		types.add(type);
	}

	public void accept(ModuleDecl module) {
		modules.add(module);
	}

	public void accept(ExtractionGap gap) {
		gaps.add(gap);
	}

	public Set<TypeDecl> getTypes() {
		return types;
	}

	public Set<ModuleDecl> getModules() {
		return modules;
	}

	public Set<ExtractionGap> getGaps() {
		return gaps;
	}
}
