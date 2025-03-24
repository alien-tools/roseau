package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveRecordStrategy extends AbstractRcdStrategy {
	public RemoveRecordStrategy(RecordDecl rcd, NewApiQueue queue) {
		super(rcd, queue, "RemoveRecord" + rcd.getSimpleName());
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing record " + rcd.getPrettyQualifiedName());

		mutableApi.allTypes.remove(rcd.getQualifiedName());
		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
