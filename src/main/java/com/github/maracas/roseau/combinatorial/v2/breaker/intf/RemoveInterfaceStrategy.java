package com.github.maracas.roseau.combinatorial.v2.breaker.intf;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.InterfaceDecl;
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

			switch (typeBuilder) {
				case ClassBuilder classBuilder: removeInterfaceFromClassBuilder(classBuilder); break;
				case InterfaceBuilder interfaceBuilder: removeInterfaceFromInterfaceBuilder(interfaceBuilder); break;
			}
		});
	}

	private void removeInterfaceFromTypeDeclBuilder(TypeDeclBuilder typeBuilder) {
		typeBuilder.implementedInterfaces = typeBuilder.implementedInterfaces.stream()
				.filter(i -> !i.getQualifiedName().equals(intf.getQualifiedName()))
				.toList();
	}

	private void removeInterfaceFromClassBuilder(ClassBuilder classBuilder) {
		classBuilder.permittedTypes = classBuilder.permittedTypes.stream()
				.filter(type -> !type.equals(intf.getQualifiedName()))
				.toList();
	}

	private void removeInterfaceFromInterfaceBuilder(InterfaceBuilder interfaceBuilder) {
		interfaceBuilder.permittedTypes = interfaceBuilder.permittedTypes.stream()
				.filter(type -> !type.equals(intf.getQualifiedName()))
				.toList();
	}
}
