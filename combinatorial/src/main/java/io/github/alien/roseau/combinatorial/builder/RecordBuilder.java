package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.RecordDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RecordBuilder extends ClassBuilder {
	public List<RecordComponentBuilder> recordComponents = new ArrayList<>();

	public RecordDecl make() {
		var fields = this.fields.stream().map(FieldBuilder::make).toList();
		var methods = this.methods.stream().map(MethodBuilder::make).toList();
		var constructors = this.constructors.stream().map(ConstructorBuilder::make).toList();
		var recordComponents = this.recordComponents.stream().map(RecordComponentBuilder::make).toList();

		return new RecordDecl(qualifiedName, visibility, modifiers, Set.copyOf(annotations), location, Set.copyOf(implementedInterfaces),
				formalTypeParameters, Set.copyOf(fields), Set.copyOf(methods), enclosingType, Set.copyOf(constructors), recordComponents);
	}

	public static RecordBuilder from(RecordDecl decl) {
		var builder = new RecordBuilder();

		builder.mutateWithDecl(decl);

		builder.recordComponents = new ArrayList<>(decl.getRecordComponents().stream().map(RecordComponentBuilder::from).toList());

		return builder;
	}
}
