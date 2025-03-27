package io.github.alien.roseau.combinatorial.v2.breaker.rcdCpt;

import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveRecordComponentStrategy extends AbstractRcdCptValStrategy {
	public RemoveRecordComponentStrategy(RecordComponentDecl rcdCpt, NewApiQueue queue) {
		super(rcdCpt, queue, "RemoveRecordComponent%sFrom%s".formatted(
				StringUtils.capitalizeFirstLetter(rcdCpt.getSimpleName()),
				rcdCpt.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(rcdCpt.getContainingType().getQualifiedName());
		if (containingType == null) throw new ImpossibleChangeException();

		if (containingType instanceof RecordBuilder recordBuilder) {
			LOGGER.info("Removing record component {} from {}", rcdCpt.getPrettyQualifiedName(), containingType.qualifiedName);

			recordBuilder.recordComponents.remove(rcdCpt);

			// TODO: For now we don't have hierarchy, so we don't need to update possible references
		} else {
			throw new ImpossibleChangeException();
		}
	}
}
