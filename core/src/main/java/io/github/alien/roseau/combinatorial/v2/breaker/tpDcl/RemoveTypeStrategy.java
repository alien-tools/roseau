package io.github.alien.roseau.combinatorial.v2.breaker.tpDcl;

import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveTypeStrategy extends AbstractTpStrategy {
	public RemoveTypeStrategy(TypeDecl tp, NewApiQueue queue) {
		super(tp, queue, "RemoveType" + tp.getSimpleName());
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing type {}", tp.getPrettyQualifiedName());

		mutableApi.allTypes.remove(tp.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
