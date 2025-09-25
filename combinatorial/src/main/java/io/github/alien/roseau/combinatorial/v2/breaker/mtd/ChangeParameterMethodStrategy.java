package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeParameterMethodStrategy extends AbstractMtdStrategy {
	private final int parameterIndex;
	private final ITypeReference parameterType;
	private final boolean parameterIsVarargs;

	public ChangeParameterMethodStrategy(int index, ITypeReference type, boolean isVarargs, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "ChangeParameter%dTo%s%sFromMethod%sIn%s".formatted(
				index,
				StringUtils.getPrettyQualifiedName(type),
				isVarargs ? "Varargs" : "",
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				StringUtils.getPrettyQualifiedName(mtd.getContainingType())),
			api
		);

		this.parameterIndex = index;
		this.parameterType = type;
		this.parameterIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		var currentParameter = method.parameters.get(parameterIndex);
		if (currentParameter == null) throw new ImpossibleChangeException();
		if (currentParameter.type.equals(parameterType) && currentParameter.isVarargs == parameterIsVarargs)
			throw new ImpossibleChangeException();

		LOGGER.info("Changing parameter at index {} from method {}", parameterIndex, tpMbr.getQualifiedName());

		currentParameter.type = parameterType;
		currentParameter.isVarargs = parameterIsVarargs;
	}
}
