package com.github.maracas.roseau.combinatorial.builder;

import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeMember;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;

sealed abstract class TypeMemberBuilder extends SymbolBuilder implements Builder<TypeMember> permits EnumValueBuilder, ExecutableBuilder, FieldBuilder, RecordComponentBuilder {
	public TypeReference<TypeDecl> containingType;
	public ITypeReference type;
}
