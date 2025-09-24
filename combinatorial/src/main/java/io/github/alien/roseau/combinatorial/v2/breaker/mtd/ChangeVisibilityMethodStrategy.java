package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityMethodStrategy extends AbstractMtdStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityMethodStrategy(AccessModifier modifier, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "ReduceMethod%sIn%sVisibilityTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				StringUtils.getPrettyQualifiedName(mtd.getContainingType()),
				StringUtils.splitSpecialCharsAndCapitalize(modifier.name())),
			api
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		LOGGER.info("Reducing method {} visibility to {}", tpMbr.getQualifiedName(), StringUtils.splitSpecialCharsAndCapitalize(accessModifier.name()));

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		method.visibility = accessModifier;
	}
}
