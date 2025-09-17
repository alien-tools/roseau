package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityConstructorStrategy extends AbstractCtrStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityConstructorStrategy(AccessModifier modifier, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "ReduceConstructor%sIn%sVisibilityTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				ctr.getContainingType().getPrettyQualifiedName(),
				modifier.toCapitalize()),
				api
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		LOGGER.info("Reducing constructor {} visibility to {}", tpMbr.getQualifiedName(), accessModifier.toCapitalize());

		var constructor = this.getConstructorFrom(mutableApi);
		constructor.visibility = accessModifier;
	}
}
