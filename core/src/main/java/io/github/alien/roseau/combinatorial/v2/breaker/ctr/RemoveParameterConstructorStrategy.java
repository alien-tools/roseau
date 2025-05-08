package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveParameterConstructorStrategy extends AbstractCtrStrategy {
	private final int parameterIndex;

	public RemoveParameterConstructorStrategy(int parameterIndex, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "RemoveParameter%dFromConstructor%sIn%s".formatted(
				parameterIndex,
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				ctr.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.parameterIndex = parameterIndex;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);
		var constructor = getConstructorFrom(containingType);
		if (parameterIndex < 0 || parameterIndex >= constructor.parameters.size()) throw new ImpossibleChangeException();

		constructor.parameters.remove(parameterIndex);
		if (areConstructorsInvalid(containingType.constructors, api)) throw new ImpossibleChangeException();

		LOGGER.info("Removing parameter at index {} from constructor {}", parameterIndex, tpMbr.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
