package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	public RemoveTypeStrategy(T tp, NewApiQueue queue) {
		super(tp, queue, "RemoveType" + tp.getSimpleName());
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing type {}", tp.getPrettyQualifiedName());

		mutableApi.allTypes.remove(tp.getQualifiedName());

		mutableApi.allTypes.values().forEach(typeBuilder -> {
			if (tp instanceof InterfaceDecl) {
				removeInterfaceFromTypeBuilder(typeBuilder);
			}

			if (typeBuilder instanceof ClassBuilder classBuilder) {
				removeTypeFromTypeBuilderPermittedTypes(classBuilder);

				if (tp instanceof ClassDecl) {
					removeClassFromTypeBuilder(classBuilder);
				}
			} else if (typeBuilder instanceof InterfaceBuilder interfaceBuilder) {
				removeTypeFromTypeBuilderPermittedTypes(interfaceBuilder);
			}
		});
	}

	private void removeClassFromTypeBuilder(ClassBuilder classBuilder) {
		if (classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName())) {
			if (classBuilder.modifiers.contains(Modifier.NON_SEALED)) {
				var hasNoOtherSealedInterfacesImplemented = classBuilder.implementedInterfaces.stream()
						.noneMatch(i -> i.getResolvedApiType().map(TypeDecl::isSealed).orElse(false));

				if (hasNoOtherSealedInterfacesImplemented) {
					classBuilder.modifiers.remove(Modifier.NON_SEALED);
				}
			}

			classBuilder.superClass = null;
		}
	}

	private void removeInterfaceFromTypeBuilder(TypeBuilder typeBuilder) {
		var implementedInterfacesCount = typeBuilder.implementedInterfaces.size();
		typeBuilder.implementedInterfaces = typeBuilder.implementedInterfaces.stream()
				.filter(i -> !i.getQualifiedName().equals(tp.getQualifiedName()))
				.toList();

		if (implementedInterfacesCount != typeBuilder.implementedInterfaces.size()) {
			if (typeBuilder.modifiers.contains(Modifier.NON_SEALED)) {
				var hasNoOtherSealedInterfacesImplemented = typeBuilder.implementedInterfaces.stream()
						.noneMatch(i -> i.getResolvedApiType().map(TypeDecl::isSealed).orElse(false));

				var hasSealedSuperClass = false;
				if (typeBuilder instanceof ClassBuilder classBuilder && classBuilder.superClass != null) {
					hasSealedSuperClass = classBuilder.superClass.getResolvedApiType().map(ClassDecl::isSealed).orElse(false);
				}

				if (hasNoOtherSealedInterfacesImplemented && !hasSealedSuperClass) {
					typeBuilder.modifiers.remove(Modifier.NON_SEALED);
				}
			}
		}
	}

	private void removeTypeFromTypeBuilderPermittedTypes(ClassBuilder classBuilder) {
		classBuilder.permittedTypes = classBuilder.permittedTypes.stream()
				.filter(type -> !type.equals(tp.getQualifiedName()))
				.toList();

		if (classBuilder.modifiers.contains(Modifier.SEALED) && classBuilder.permittedTypes.isEmpty()) {
			classBuilder.modifiers.remove(Modifier.SEALED);
		}
	}

	private void removeTypeFromTypeBuilderPermittedTypes(InterfaceBuilder interfaceBuilder) {
		interfaceBuilder.permittedTypes = interfaceBuilder.permittedTypes.stream()
				.filter(type -> !type.equals(tp.getQualifiedName()))
				.toList();

		if (interfaceBuilder.modifiers.contains(Modifier.SEALED) && interfaceBuilder.permittedTypes.isEmpty()) {
			interfaceBuilder.modifiers.remove(Modifier.SEALED);
		}
	}
}
