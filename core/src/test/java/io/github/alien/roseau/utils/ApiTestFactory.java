package io.github.alien.roseau.utils;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

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

	public static FieldDecl newField(String containing, String fqn, ITypeReference type, Set<Modifier> modifiers) {
		return newField(fqn, new TypeReference<>(containing), type, modifiers);
	}

	public static FieldDecl newField(String fqn, TypeReference<TypeDecl> containingType, ITypeReference type,
	                                 Set<Modifier> modifiers) {
		return new FieldDecl(fqn, AccessModifier.PUBLIC, modifiers, Set.of(), SourceLocation.NO_LOCATION,
			containingType, type, false);
	}

	public static MethodDecl newMethod(String containing, String fqn, ITypeReference returnType, List<ParameterDecl> parameters,
	                                   Set<Modifier> modifiers) {
		return newMethod(fqn, new TypeReference<>(containing), returnType, parameters, modifiers);
	}

	public static MethodDecl newMethod(String fqn, TypeReference<TypeDecl> containingType, ITypeReference returnType,
	                                   List<ParameterDecl> parameters, Set<Modifier> modifiers) {
		return new MethodDecl(fqn, AccessModifier.PUBLIC, modifiers, Set.of(), SourceLocation.NO_LOCATION,
			containingType, returnType, parameters, List.of(), Set.of());
	}

	public static ConstructorDecl newConstructor(String fqn, TypeReference<TypeDecl> containingType,
	                                             List<ParameterDecl> parameters) {
		return new ConstructorDecl(fqn, AccessModifier.PUBLIC, Set.of(), Set.of(), SourceLocation.NO_LOCATION,
			containingType, containingType, parameters, List.of(), Set.of());
	}

	public static ParameterDecl newParameter(String name, ITypeReference type) {
		return new ParameterDecl(name, type, false);
	}

	public static ParameterDecl newVarargsParameter(String name, ITypeReference type) {
		return new ParameterDecl(name, type, true);
	}
}
