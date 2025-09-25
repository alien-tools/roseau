package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddImplementedInterfaceTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	public AddImplementedInterfaceTypeStrategy(T tp, NewApiQueue queue, API api) {
		super(tp, queue, "AddImplementedInterfaceToType%s".formatted(tp.getSimpleName()), api);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Adding interface to type {}", tp.getQualifiedName());

		var newInterface = new InterfaceBuilder();
		newInterface.qualifiedName = "api.NewInterface";
		newInterface.visibility = AccessModifier.PUBLIC;

		var mutableType = getMutableType(mutableApi);
		mutableApi.allTypes.put(newInterface.qualifiedName, newInterface);
		mutableType.implementedInterfaces.add(mutableApi.typeReferenceFactory.createTypeReference(newInterface.qualifiedName));
	}
}
