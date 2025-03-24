package io.github.alien.roseau.combinatorial.v2.breaker.enm;

import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveEnumStrategy extends AbstractEnmStrategy {
	public RemoveEnumStrategy(EnumDecl enm, NewApiQueue queue) {
		super(enm, queue, "RemoveEnum" + enm.getSimpleName());
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing enum " + enm.getPrettyQualifiedName());

		mutableApi.allTypes.remove(enm.getQualifiedName());
		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
