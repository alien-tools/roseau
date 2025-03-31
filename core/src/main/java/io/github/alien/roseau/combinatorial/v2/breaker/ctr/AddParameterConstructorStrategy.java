package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ParameterBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddParameterConstructorStrategy extends AbstractCtrStrategy {
	private final ParameterBuilder parameter;

	public AddParameterConstructorStrategy(ParameterBuilder parameter, ConstructorDecl ctr, NewApiQueue queue) {
		super(ctr, queue, "AddParameter%s%sToConstructor%sIn%s".formatted(
				parameter.type.getPrettyQualifiedName(),
				parameter.isVarargs ? "Varargs" : "",
				StringUtils.splitSpecialCharsAndCapitalize(ctr.getErasure()),
				ctr.getContainingType().getPrettyQualifiedName())
		);

		this.parameter = parameter;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);
		var constructor = getConstructorFrom(mutableApi);
		if (!constructor.parameters.isEmpty() && constructor.parameters.getLast().isVarargs) throw new ImpossibleChangeException();

		constructor.parameters.add(parameter);
		if (areConstructorsInvalid(containingType.constructors)) throw new ImpossibleChangeException();

		LOGGER.info("Adding parameter {} to constructor {}", parameter.type.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
