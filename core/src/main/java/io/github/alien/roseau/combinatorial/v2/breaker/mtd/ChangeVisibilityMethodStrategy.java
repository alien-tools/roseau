package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityMethodStrategy extends AbstractMtdStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityMethodStrategy(AccessModifier modifier, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "ReduceMethod%sIn%sVisibilityTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName(),
				modifier.toCapitalize())
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		LOGGER.info("Reducing method {} visibility to {}", tpMbr.getQualifiedName(), accessModifier.toCapitalize());

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		method.visibility = accessModifier;
	}
}
