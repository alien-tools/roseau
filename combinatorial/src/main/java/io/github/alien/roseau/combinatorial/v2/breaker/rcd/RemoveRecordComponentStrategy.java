package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveRecordComponentStrategy extends AbstractRcdStrategy {
	private final int recordComponentIndex;

	public RemoveRecordComponentStrategy(int index, RecordDecl rcd, NewApiQueue queue, API api) {
		super(rcd, queue, "RemoveRecordComponent%dFromRecord%s".formatted(index, rcd.getPrettyQualifiedName()), api);

		this.recordComponentIndex = index;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing record component at index {} from record {}", recordComponentIndex, tp.getQualifiedName());

		var mutableRecord = getMutableBuilderFromMutableApi(mutableApi);
		mutableRecord.recordComponents.remove(recordComponentIndex);
	}
}
