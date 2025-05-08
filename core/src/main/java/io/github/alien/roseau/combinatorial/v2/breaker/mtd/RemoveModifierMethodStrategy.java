package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierMethodStrategy extends AbstractMtdStrategy {
	private final Modifier modifier;

	public RemoveModifierMethodStrategy(Modifier modifier, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "RemoveModifier%sToMethod%sIn%s".formatted(
				modifier.toCapitalize(),
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				mtd.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tpMbr.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		var method = getMethodFrom(mutableApi);

		LOGGER.info("Removing modifier {} to method {}", modifier.toCapitalize(), tpMbr.getQualifiedName());

		method.modifiers.remove(modifier);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
