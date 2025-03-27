package io.github.alien.roseau.combinatorial.v2.breaker.enmVal;

import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.EnumBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveEnumValueStrategy extends AbstractEnmValStrategy {
	public RemoveEnumValueStrategy(EnumValueDecl enmVal, NewApiQueue queue) {
		super(enmVal, queue, "RemoveEnumValue%sFrom%s".formatted(
				StringUtils.capitalizeFirstLetter(enmVal.getSimpleName()),
				enmVal.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(enmVal.getContainingType().getQualifiedName());
		if (containingType == null) throw new ImpossibleChangeException();

		if (containingType instanceof EnumBuilder enumBuilder) {
			LOGGER.info("Removing enum value {} from {}", enmVal.getPrettyQualifiedName(), containingType.qualifiedName);

			enumBuilder.values.remove(enmVal);

			// TODO: For now we don't have hierarchy, so we don't need to update possible references
		} else {
			throw new ImpossibleChangeException();
		}
	}
}
