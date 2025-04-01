package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
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
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		if (parameterIndex < 0 || parameterIndex >= method.parameters.size()) throw new ImpossibleChangeException();
		if (parameterIsVarargs && parameterIndex != method.parameters.size() - 1) throw new ImpossibleChangeException();

		var currentParameter = method.parameters.get(parameterIndex);
		if (currentParameter == null) throw new ImpossibleChangeException();
		if (currentParameter.type.equals(parameterType) && currentParameter.isVarargs == parameterIsVarargs) throw new ImpossibleChangeException();

		currentParameter.type = parameterType;
		currentParameter.isVarargs = parameterIsVarargs;
		if (areMethodsInvalid(containingType.methods)) throw new ImpossibleChangeException();

		LOGGER.info("Changing parameter at index {} from method {}", parameterIndex, tpMbr.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
