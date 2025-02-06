package com.github.maracas.roseau.combinatorial.v2.breaker.intf;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.combinatorial.builder.*;
import com.github.maracas.roseau.combinatorial.v2.NewApiQueue;

public final class RemoveInterfaceStrategy extends AbstractIntfStrategy {
	public RemoveInterfaceStrategy(InterfaceDecl intf, NewApiQueue queue) {
		super(intf, queue, "RemoveInterface" + intf.getSimpleName());
	}

	@Override
	protected void applyBreakToMutableApi(API api, ApiBuilder mutableApi) {
		System.out.println("Removing interface " + intf.getPrettyQualifiedName());

		mutableApi.allTypes.remove(intf.getQualifiedName());
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
				.filter(i -> !i.getQualifiedName().equals(intf.getQualifiedName()))
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
				.filter(type -> !type.equals(intf.getQualifiedName()))
				.toList();

		if (interfaceBuilder.modifiers.contains(Modifier.SEALED) && interfaceBuilder.permittedTypes.isEmpty()) {
			interfaceBuilder.modifiers.remove(Modifier.SEALED);
		}
	}
}
