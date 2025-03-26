package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.model.RecordDecl;

import java.util.ArrayList;
import java.util.List;

public final class RecordBuilder extends ClassBuilder {
	public List<RecordComponentDecl> recordComponents = new ArrayList<>();

	public RecordDecl make() {
		return new RecordDecl(qualifiedName, visibility, modifiers, annotations, location, implementedInterfaces,
				formalTypeParameters, fields, methods, enclosingType, constructors, recordComponents);
	}

	public static RecordBuilder from(RecordDecl decl) {
		var builder = new RecordBuilder();

		ClassBuilder.mutateTypeDeclBuilderWithTypeDecl(builder, decl);

		builder.recordComponents = new ArrayList<>(decl.getRecordComponents());

		return builder;
	}
}
