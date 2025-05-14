package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeParameterMethodStrategy extends AbstractMtdStrategy {
	private final int parameterIndex;
	private final ITypeReference parameterType;
	private final boolean parameterIsVarargs;

	public ChangeParameterMethodStrategy(int index, ITypeReference type, boolean isVarargs, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "ChangeParameter%dTo%s%sFromMethod%sIn%s".formatted(
				index,
				type.getPrettyQualifiedName(),
				isVarargs ? "Varargs" : "",
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.parameterIndex = index;
		this.parameterType = type;
		this.parameterIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Changing parameter at index {} from method {}", parameterIndex, tpMbr.getQualifiedName());

		var containingType = getContainingClassFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		var currentParameter = method.parameters.get(parameterIndex);
		currentParameter.type = parameterType;
		currentParameter.isVarargs = parameterIsVarargs;
	}
}
