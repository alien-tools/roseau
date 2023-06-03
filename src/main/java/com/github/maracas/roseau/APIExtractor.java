package com.github.maracas.roseau;

import com.github.maracas.roseau.model.API;
import com.github.maracas.roseau.model.ConstructorDeclaration;
import com.github.maracas.roseau.model.FieldDeclaration;
import com.github.maracas.roseau.model.MethodDeclaration;
import com.github.maracas.roseau.model.TypeDeclaration;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * JLS 17
 *   - We don't care about package-private stuff
 */
public class APIExtractor {
	private final CtModel model;

	public APIExtractor(CtModel model) {
		this.model = Objects.requireNonNull(model);
	}

	public API getAPI() {
		return new API(getAccessibleTypes());
	}

	public List<TypeDeclaration> getAccessibleTypes() {
		return getAccessiblePackages().stream()
			.map(this::getAccessibleTypes)
			.flatMap(List::stream)
			.toList();
	}

	public List<TypeDeclaration> getAccessibleTypes(CtPackage pkg) {
		return pkg.getTypes().stream()
			.filter(CtType::isPublic)
			.flatMap(type -> Stream.concat(Stream.of(type), getAccessibleTypes(type).stream()))
			.map(t -> new TypeDeclaration(t,
				getAccessibleConstructors(t),
				getAccessibleMethods(t),
				getAccessibleFields(t))
			)
			.toList();
	}

	public List<FieldDeclaration> getAccessibleFields(CtType<?> type) {
		return type.getFields().stream()
			.filter(field -> field.isPublic() || (field.isProtected() && isExtensible(type)))
			.map(FieldDeclaration::of)
			.toList();
	}

	public List<MethodDeclaration> getAccessibleMethods(CtType<?> type) {
		return type.getMethods().stream()
			.filter(method -> method.isPublic() || (method.isProtected() && isExtensible(type)))
			.map(MethodDeclaration::of)
			.toList();
	}

	public List<ConstructorDeclaration> getAccessibleConstructors(CtType<?> type) {
		if (type instanceof CtClass<?> cls) {
			return new ArrayList<>(cls.getConstructors().stream()
				.filter(cons -> cons.isPublic() || (cons.isProtected() && isExtensible(type)))
				.map(ConstructorDeclaration::of)
				.toList());
		}

		return Collections.emptyList();
	}

	private List<CtPackage> getAccessiblePackages() {
		List<CtPackage> exportedPackages = new ArrayList<>();

		model.getAllModules().forEach(m -> {
			// §7.7.5: The unnamed module exports and opens every package it contains
			if (m.isUnnamedModule()) {
				exportedPackages.addAll(getAccessiblePackages(m.getRootPackage()));
			}
			else {
				// TODO: implement proper module semantics
				exportedPackages.addAll(
					m.getExportedPackages().stream()
						.map(pkg -> pkg.getPackageReference().getDeclaration())
						.toList()
				);
			}
		});

		return exportedPackages;
	}

	private List<CtPackage> getAccessiblePackages(CtPackage pkg) {
		return Stream.concat(
			Stream.of(pkg),
			pkg.getPackages().stream()
				.map(this::getAccessiblePackages)
				.flatMap(List::stream)
		).toList();
	}

	private List<CtType<?>> getAccessibleTypes(CtType<?> type) {
		return type.getNestedTypes().stream()
			.filter(nestedType -> nestedType.isPublic() || (nestedType.isProtected() && isExtensible(type)))
			.flatMap(nestedType -> Stream.concat(Stream.of(nestedType), getAccessibleTypes(nestedType).stream()))
			.toList();
	}

	private boolean isExtensible(CtType<?> type) {
		if (type.isFinal())
			return false;
		if (type instanceof CtSealable sealable)
			return sealable.getPermittedTypes().isEmpty();
		return true;
	}
}
