package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveParameterMethodStrategy extends AbstractMtdStrategy {
	private final int parameterIndex;

	public RemoveParameterMethodStrategy(int parameterIndex, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "RemoveParameter%dFromMethod%sIn%s".formatted(
				parameterIndex,
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.parameterIndex = parameterIndex;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);
		var method = getMethodFrom(mutableApi);
		if (parameterIndex < 0 || parameterIndex >= method.parameters.size()) throw new ImpossibleChangeException();

		method.parameters.remove(parameterIndex);
		if (areMethodsInvalid(containingType.methods)) throw new ImpossibleChangeException();

		LOGGER.info("Removing parameter at index {} from method {}", parameterIndex, tpMbr.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
