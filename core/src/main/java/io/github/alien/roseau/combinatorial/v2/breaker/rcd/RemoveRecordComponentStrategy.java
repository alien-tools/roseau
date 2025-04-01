package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveRecordComponentStrategy extends AbstractRcdStrategy {
	private final int recordComponentIndex;

	public RemoveRecordComponentStrategy(int index, RecordDecl rcd, NewApiQueue queue) {
		super(rcd, queue, "RemoveRecordComponent%dFromRecord%s".formatted(index, rcd.getPrettyQualifiedName()));

		this.recordComponentIndex = index;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var recordComponents = tp.getRecordComponents();
		if (recordComponentIndex < 0 || recordComponentIndex >= recordComponents.size()) throw new ImpossibleChangeException();

		var mutableRecord = getMutableBuilderFromMutableApi(mutableApi);

		LOGGER.info("Removing record component at index {} from record {}", recordComponentIndex, tp.getQualifiedName());

		mutableRecord.recordComponents.remove(recordComponentIndex);
	}
}
