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
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (mtd.getVisibility() == accessModifier) throw new ImpossibleChangeException();
		if (mtd.isAbstract() && accessModifier == AccessModifier.PRIVATE) throw new ImpossibleChangeException();

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof InterfaceBuilder && (accessModifier == AccessModifier.PRIVATE || accessModifier == AccessModifier.PROTECTED))
			throw new ImpossibleChangeException();

		var method = getMethodFrom(containingType);

		LOGGER.info("Reducing method {} visibility to {}", mtd.getQualifiedName(), accessModifier.toCapitalize());

		method.visibility = accessModifier;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
