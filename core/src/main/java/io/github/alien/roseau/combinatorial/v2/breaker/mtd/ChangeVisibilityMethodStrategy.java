package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityMethodStrategy extends AbstractMtdStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityMethodStrategy(AccessModifier modifier, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "ReduceMethod%sIn%sVisibilityTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				mtd.getContainingType().getPrettyQualifiedName(),
				modifier.toCapitalize()),
				api
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tpMbr.getVisibility() == accessModifier) throw new ImpossibleChangeException();
		if (tpMbr.isAbstract() && accessModifier == AccessModifier.PRIVATE) throw new ImpossibleChangeException();

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof InterfaceBuilder && (accessModifier == AccessModifier.PRIVATE || accessModifier == AccessModifier.PROTECTED))
			throw new ImpossibleChangeException();

		var method = getMethodFrom(containingType);

		LOGGER.info("Reducing method {} visibility to {}", tpMbr.getQualifiedName(), accessModifier.toCapitalize());

		method.visibility = accessModifier;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
