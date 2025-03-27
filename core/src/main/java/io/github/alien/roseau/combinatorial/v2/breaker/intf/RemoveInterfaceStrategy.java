package io.github.alien.roseau.combinatorial.v2.breaker.intf;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeDeclBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.RemoveTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveInterfaceStrategy extends RemoveTypeStrategy {
	public RemoveInterfaceStrategy(InterfaceDecl intf, NewApiQueue queue) {
		super(intf, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		super.applyBreakToMutableApi(mutableApi);

		mutableApi.allTypes.values().forEach(typeBuilder -> {
			removeInterfaceFromTypeDeclBuilder(typeBuilder);

			if (typeBuilder instanceof InterfaceBuilder interfaceBuilder) {
				removeInterfaceFromInterfaceBuilder(interfaceBuilder);
			}
		});
	}

	private void removeInterfaceFromTypeDeclBuilder(TypeDeclBuilder typeBuilder) {
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

	private void removeInterfaceFromInterfaceBuilder(InterfaceBuilder interfaceBuilder) {
		interfaceBuilder.permittedTypes = interfaceBuilder.permittedTypes.stream()
				.filter(type -> !type.equals(tp.getQualifiedName()))
				.toList();

		if (interfaceBuilder.modifiers.contains(Modifier.SEALED) && interfaceBuilder.permittedTypes.isEmpty()) {
			interfaceBuilder.modifiers.remove(Modifier.SEALED);
		}
	}
}
