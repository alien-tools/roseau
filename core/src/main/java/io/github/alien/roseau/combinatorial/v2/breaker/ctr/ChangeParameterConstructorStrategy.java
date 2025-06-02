package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeParameterConstructorStrategy extends AbstractCtrStrategy {
	private final int parameterIndex;
	private final ITypeReference parameterType;
	private final boolean parameterIsVarargs;

	public ChangeParameterConstructorStrategy(int index, ITypeReference type, boolean isVarargs, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "ChangeParameter%dTo%s%sFromConstructor%sIn%s".formatted(
				index,
				type.getPrettyQualifiedName(),
				isVarargs ? "Varargs" : "",
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				ctr.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.parameterIndex = index;
		this.parameterType = type;
		this.parameterIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingClassFromMutableApi(mutableApi);
		var constructor = getConstructorFrom(containingType);
		var currentParameter = constructor.parameters.get(parameterIndex);
		if (currentParameter == null) throw new ImpossibleChangeException();
		if (currentParameter.type.equals(parameterType) && currentParameter.isVarargs == parameterIsVarargs) throw new ImpossibleChangeException();

		LOGGER.info("Changing parameter at index {} from constructor {}", parameterIndex, tpMbr.getQualifiedName());

		currentParameter.type = parameterType;
		currentParameter.isVarargs = parameterIsVarargs;
	}
}
