package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ParameterBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddParameterConstructorStrategy extends AbstractCtrStrategy {
	private final ITypeReference parameterType;
	private final boolean parameterIsVarargs;

	public AddParameterConstructorStrategy(ITypeReference type, boolean isVarargs, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "AddParameter%s%sToConstructor%sIn%s".formatted(
				type.getPrettyQualifiedName(),
				isVarargs ? "Varargs" : "",
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				ctr.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.parameterType = type;
		this.parameterIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);
		var constructor = getConstructorFrom(containingType);
		if (!constructor.parameters.isEmpty() && constructor.parameters.getLast().isVarargs) throw new ImpossibleChangeException();

		var paramBuilder = new ParameterBuilder();
		paramBuilder.name = "newParamAdded";
		paramBuilder.type = parameterType;
		paramBuilder.isVarargs = parameterIsVarargs;
		constructor.parameters.add(paramBuilder);
		if (areConstructorsInvalid(containingType.constructors, api)) throw new ImpossibleChangeException();

		LOGGER.info("Adding parameter {} to constructor {}", parameterType.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
