package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityConstructorStrategy extends AbstractCtrStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityConstructorStrategy(AccessModifier modifier, ConstructorDecl ctr, NewApiQueue queue) {
		super(ctr, queue, "ReduceConstructor%sFrom%sVisibilityTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(ctr.getErasure()),
				ctr.getContainingType().getPrettyQualifiedName(),
				modifier.toCapitalize())
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (ctr.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		var constructor = this.getConstructorFrom(mutableApi);

		LOGGER.info("Reducing constructor {} visibility to {}", ctr.getQualifiedName(), accessModifier.toCapitalize());

		constructor.visibility = accessModifier;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
