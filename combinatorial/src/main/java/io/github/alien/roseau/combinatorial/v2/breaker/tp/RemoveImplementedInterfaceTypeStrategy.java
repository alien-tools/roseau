package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveImplementedInterfaceTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	private final String implementedInterfaceQualifiedName;

	public RemoveImplementedInterfaceTypeStrategy(String implementedInterfaceQualifiedName, T tp, NewApiQueue queue, API api) {
		super(tp, queue, "RemoveImplementedInterface%sFromType%s".formatted(implementedInterfaceQualifiedName, tp.getSimpleName()), api);

		this.implementedInterfaceQualifiedName = implementedInterfaceQualifiedName;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing interface from type {}", tp.getQualifiedName());

		var mutableType = getMutableType(mutableApi);
		mutableType.implementedInterfaces.removeIf(tR -> tR.getQualifiedName().equals(implementedInterfaceQualifiedName));
	}
}
