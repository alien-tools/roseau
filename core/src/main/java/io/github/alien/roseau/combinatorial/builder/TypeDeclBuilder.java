package io.github.alien.roseau.combinatorial.builder;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;

import java.util.ArrayList;
import java.util.List;

public abstract sealed class TypeDeclBuilder extends SymbolBuilder implements Builder<TypeDecl> permits ClassBuilder, InterfaceBuilder {
	public List<TypeReference<InterfaceDecl>> implementedInterfaces = new ArrayList<>();
	public List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	public List<FieldDecl> fields = new ArrayList<>();
	public List<MethodDecl> methods = new ArrayList<>();
	public TypeReference<TypeDecl> enclosingType;

	public static void mutateTypeDeclBuilderWithTypeDecl(TypeDeclBuilder builder, TypeDecl typeDecl) {
		SymbolBuilder.mutateSymbolBuilderWithSymbol(builder, typeDecl);

		builder.implementedInterfaces = new ArrayList<>(typeDecl.getImplementedInterfaces());
		builder.formalTypeParameters = new ArrayList<>(typeDecl.getFormalTypeParameters());
		builder.fields = new ArrayList<>(typeDecl.getDeclaredFields());
		builder.methods = new ArrayList<>(typeDecl.getDeclaredMethods());
		builder.enclosingType = typeDecl.getEnclosingType().orElse(null);
	}
}
