package io.github.alien.roseau.extractors.jdt;

import io.github.alien.roseau.api.model.ModuleDecl;
import io.github.alien.roseau.api.model.TypeDecl;

import java.util.Set;

record ParsingResult(
	Set<TypeDecl> types,
	Set<ModuleDecl> modules
) {
	ParsingResult {
		types = Set.copyOf(types);
		modules = Set.copyOf(modules);
	}
}
