package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveImplementedInterfaceTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	private final InterfaceDecl implementedInterfaceDecl;

	public RemoveImplementedInterfaceTypeStrategy(InterfaceDecl implementedInterfaceDecl, T tp, NewApiQueue queue) {
		super(tp, queue, "RemoveImplementedInterface%sFromType%s".formatted(implementedInterfaceDecl.getPrettyQualifiedName(), tp.getSimpleName()));

		this.implementedInterfaceDecl = implementedInterfaceDecl;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing interface from type {}", tp.getQualifiedName());

		var mutableType = getMutableType(mutableApi);
		mutableType.implementedInterfaces.removeIf(tR -> tR.getQualifiedName().equals(implementedInterfaceDecl.getQualifiedName()));
	}
}
