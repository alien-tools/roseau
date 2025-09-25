package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierMethodStrategy extends AbstractMtdStrategy {
	private final Modifier modifier;

	public RemoveModifierMethodStrategy(Modifier modifier, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "RemoveModifier%sToMethod%sIn%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(modifier.name()),
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				StringUtils.getPrettyQualifiedName(mtd.getContainingType())),
			api
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (!tpMbr.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Removing modifier {} to method {}", StringUtils.splitSpecialCharsAndCapitalize(modifier.name()), tpMbr.getQualifiedName());

		var method = getMethodFrom(mutableApi);
		method.modifiers.remove(modifier);
	}
}
