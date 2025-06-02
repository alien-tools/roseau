package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierMethodStrategy extends AbstractMtdStrategy {
	private final Modifier modifier;

	public AddModifierMethodStrategy(Modifier modifier, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "AddModifier%sToMethod%sIn%s".formatted(
				modifier.toCapitalize(),
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				mtd.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Adding modifier {} to method {}", modifier.toCapitalize(), tpMbr.getQualifiedName());

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		method.modifiers.add(modifier);
	}
}
