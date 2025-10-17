package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public sealed class ClassBuilder extends TypeBuilder permits EnumBuilder, RecordBuilder {
	public TypeReference<ClassDecl> superClass;
	public List<ConstructorBuilder> constructors = new ArrayList<>();
	public List<TypeReference<TypeDecl>> permittedTypes = new ArrayList<>();

	public ClassDecl make() {
		var fields = this.fields.stream().map(FieldBuilder::make).toList();
		var methods = this.methods.stream().map(MethodBuilder::make).toList();
		var constructors = this.constructors.stream().map(ConstructorBuilder::make).toList();

		return new ClassDecl(qualifiedName, visibility, modifiers, Set.copyOf(annotations), location,
				Set.copyOf(implementedInterfaces), formalTypeParameters, Set.copyOf(fields), Set.copyOf(methods), enclosingType, superClass,
				Set.copyOf(constructors), Set.copyOf(permittedTypes));
	}

	public static ClassBuilder from(ClassDecl decl) {
		var builder = new ClassBuilder();

		builder.mutateWithDecl(decl);

		return builder;
	}

	protected void mutateWithDecl(ClassDecl decl) {
		super.mutateWithDecl(decl);

		superClass = decl.getSuperClass();
		constructors = new ArrayList<>(decl.getDeclaredConstructors().stream().map(ConstructorBuilder::from).toList());
		permittedTypes = new ArrayList<>(decl.getPermittedTypes());
	}
}
