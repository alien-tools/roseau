package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	public RemoveTypeStrategy(T tp, NewApiQueue queue, API api) {
		super(tp, queue, "RemoveType" + tp.getSimpleName(), api);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing type {}", tp.getPrettyQualifiedName());

		mutableApi.allTypes.remove(tp.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
