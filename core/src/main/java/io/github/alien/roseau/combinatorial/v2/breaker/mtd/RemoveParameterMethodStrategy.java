package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
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
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing parameter at index {} from method {}", parameterIndex, tpMbr.getQualifiedName());

		var containingType = getContainingClassFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		method.parameters.remove(parameterIndex);
	}
}
