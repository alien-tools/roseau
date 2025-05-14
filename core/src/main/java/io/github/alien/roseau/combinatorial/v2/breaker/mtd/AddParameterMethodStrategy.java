package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ParameterBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddParameterMethodStrategy extends AbstractMtdStrategy {
	private final ITypeReference parameterType;
	private final boolean parameterIsVarargs;

	public AddParameterMethodStrategy(ITypeReference type, boolean isVarargs, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "AddParameter%s%sToMethod%sIn%s".formatted(
				type.getPrettyQualifiedName(),
				isVarargs ? "Varargs" : "",
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.parameterType = type;
		this.parameterIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Adding parameter {} to method {}", parameterType.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		var paramBuilder = new ParameterBuilder();
		paramBuilder.name = "newParamAdded";
		paramBuilder.type = parameterType;
		paramBuilder.isVarargs = parameterIsVarargs;

		var containingType = getContainingClassFromMutableApi(mutableApi);
		var method = getMethodFrom(containingType);
		method.parameters.add(paramBuilder);
	}
}
