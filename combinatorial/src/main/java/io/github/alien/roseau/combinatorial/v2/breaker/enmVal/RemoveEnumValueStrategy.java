package io.github.alien.roseau.combinatorial.v2.breaker.enmVal;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveEnumValueStrategy extends AbstractEnmValStrategy {
	public RemoveEnumValueStrategy(EnumValueDecl enmVal, NewApiQueue queue, API api) {
		super(enmVal, queue, "RemoveEnumValue%sIn%s".formatted(
				StringUtils.capitalizeFirstLetter(enmVal.getSimpleName()),
				enmVal.getContainingType().getPrettyQualifiedName()),
				api
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing enum value {}", tpMbr.getPrettyQualifiedName());

		var containingEnum = getContainingEnumFromMutableApi(mutableApi);
		containingEnum.values = containingEnum.values.stream().filter(e -> !e.make().equals(tpMbr)).toList();
	}
}
