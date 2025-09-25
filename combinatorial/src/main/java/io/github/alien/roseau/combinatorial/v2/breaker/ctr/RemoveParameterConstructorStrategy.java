package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveParameterConstructorStrategy extends AbstractCtrStrategy {
	private final int parameterIndex;

	public RemoveParameterConstructorStrategy(int parameterIndex, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "RemoveParameter%dFromConstructor%sIn%s".formatted(
				parameterIndex,
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				StringUtils.getPrettyQualifiedName(ctr.getContainingType())),
			api
		);

		this.parameterIndex = parameterIndex;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing parameter at index {} from constructor {}", parameterIndex, tpMbr.getQualifiedName());

		var containingType = getContainingClassFromMutableApi(mutableApi);
		var constructor = getConstructorFrom(containingType);
		constructor.parameters.remove(parameterIndex);
	}
}
