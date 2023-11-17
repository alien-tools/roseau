package com.github.maracas.roseau.api;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.api.model.TypeReference;
import com.github.maracas.roseau.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.visit.Visit;

import java.util.Optional;

public class TypeResolver extends AbstractAPIVisitor {
	final API api;

	public TypeResolver(API api) {
		this.api = api;
	}

	@Override
	public Visit typeReference(TypeReference<TypeDecl> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			Optional<TypeDecl> resolved = api.getAllTypes().stream()
				.filter(t -> t.getQualifiedName().equals(it.getQualifiedName()))
				.findFirst();

			resolved.ifPresentOrElse(
				it::setActualType,
				() -> {}
			);
		};
	}

	@Override
	public Visit classReference(TypeReference<ClassDecl> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			Optional<ClassDecl> resolved = api.getExportedClasses().stream()
				.filter(t -> t.getQualifiedName().equals(it.getQualifiedName()))
				.findFirst();

			resolved.ifPresentOrElse(
				it::setActualType,
				() -> {}
			);
		};
	}

	@Override
	public Visit interfaceReference(TypeReference<InterfaceDecl> it) {
		return () -> {
			if (it.getActualType().isPresent())
				return;

			Optional<InterfaceDecl> resolved = api.getExportedInterfaces().stream()
				.filter(t -> t.getQualifiedName().equals(it.getQualifiedName()))
				.findFirst();

			resolved.ifPresentOrElse(
				it::setActualType,
				() -> {}
			);
		};
	}
}
