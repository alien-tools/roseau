package io.github.alien.roseau.combinatorial.v2.breaker.rcdCpt;

import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveRecordComponentStrategy extends AbstractRcdCptValStrategy {
	public RemoveRecordComponentStrategy(RecordComponentDecl rcdCpt, NewApiQueue queue) {
		super(rcdCpt, queue, "RemoveRecordComponent%sIn%s".formatted(
				StringUtils.capitalizeFirstLetter(rcdCpt.getSimpleName()),
				rcdCpt.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingRecord = getContainingRecordFromMutableApi(mutableApi);

		containingRecord.recordComponents = containingRecord.recordComponents.stream().filter(r -> !r.make().equals(rcdCpt)).toList();

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
