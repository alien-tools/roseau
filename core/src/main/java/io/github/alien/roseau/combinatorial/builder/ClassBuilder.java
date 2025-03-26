package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;

public sealed class ClassBuilder extends TypeDeclBuilder permits EnumBuilder, RecordBuilder {
	public TypeReference<ClassDecl> superClass;
	public List<ConstructorDecl> constructors = new ArrayList<>();
	public List<String> permittedTypes = new ArrayList<>();

	public ClassDecl make() {
		return new ClassDecl(qualifiedName, visibility, modifiers, annotations, location,
				implementedInterfaces, formalTypeParameters, fields, methods, enclosingType, superClass,
				constructors, permittedTypes);
	}

	public static ClassBuilder from(ClassDecl decl) {
		var builder = new ClassBuilder();

		TypeDeclBuilder.mutateTypeDeclBuilderWithTypeDecl(builder, decl);

		builder.superClass = decl.getSuperClass();
		builder.constructors = new ArrayList<>(decl.getDeclaredConstructors());
		builder.permittedTypes = new ArrayList<>(decl.getPermittedTypes());

		return builder;
	}
}
