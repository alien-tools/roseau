package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;

import java.util.List;
import java.util.Set;

public class ApiTestFactory {
	public static ClassDecl newClass(String fqn, AccessModifier visibility) {
		return new ClassDecl(fqn, visibility, Set.of(), Set.of(), SourceLocation.NO_LOCATION,
			Set.of(), List.of(), Set.of(), Set.of(), null, null, Set.of(), Set.of());
	}

	public static InterfaceDecl newInterface(String fqn, AccessModifier visibility) {
		return new InterfaceDecl(fqn, visibility, Set.of(), Set.of(), SourceLocation.NO_LOCATION,
			Set.of(), List.of(), Set.of(), Set.of(), null, Set.of());
	}

	public static EnumDecl newEnum(String fqn, AccessModifier visibility) {
		return new EnumDecl(fqn, visibility, Set.of(), Set.of(), SourceLocation.NO_LOCATION,
			Set.of(), Set.of(), Set.of(), null, Set.of(), Set.of());
	}

	public static RecordDecl newRecord(String fqn, AccessModifier visibility) {
		return new RecordDecl(fqn, visibility, Set.of(), Set.of(), SourceLocation.NO_LOCATION,
			Set.of(), List.of(), Set.of(), Set.of(), null, Set.of(), List.of());
	}
}
