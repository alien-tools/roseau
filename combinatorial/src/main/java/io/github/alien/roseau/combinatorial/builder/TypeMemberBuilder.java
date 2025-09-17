package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;

sealed abstract class TypeMemberBuilder extends SymbolBuilder implements Builder<TypeMemberDecl> permits EnumValueBuilder, ExecutableBuilder, FieldBuilder, RecordComponentBuilder {
	public TypeReference<TypeDecl> containingType;
	public ITypeReference type;

	protected void mutateWithDecl(TypeMemberDecl typeMemberDecl) {
		super.mutateWithDecl(typeMemberDecl);

		containingType = typeMemberDecl.getContainingType();
		type = typeMemberDecl.getType();
	}
}
