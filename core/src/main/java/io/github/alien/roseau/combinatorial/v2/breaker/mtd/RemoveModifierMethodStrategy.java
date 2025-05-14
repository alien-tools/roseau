package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierMethodStrategy extends AbstractMtdStrategy {
	private final Modifier modifier;

	public RemoveModifierMethodStrategy(Modifier modifier, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "RemoveModifier%sToMethod%sIn%s".formatted(
				modifier.toCapitalize(),
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing modifier {} to method {}", modifier.toCapitalize(), tpMbr.getQualifiedName());

		var method = getMethodFrom(mutableApi);
		method.modifiers.remove(modifier);
	}
}
