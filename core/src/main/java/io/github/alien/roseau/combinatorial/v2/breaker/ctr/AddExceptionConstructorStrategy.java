package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddExceptionConstructorStrategy extends AbstractCtrStrategy {
	private final ITypeReference exception;

	public AddExceptionConstructorStrategy(ITypeReference exception, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "AddException%sToConstructor%sIn%s".formatted(
				exception.getPrettyQualifiedName(),
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				ctr.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.exception = exception;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getThrownExceptions().contains(exception)) throw new ImpossibleChangeException();

		LOGGER.info("Adding exception {} to constructor {}", exception.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		var constructor = getConstructorFrom(mutableApi);
		constructor.thrownExceptions.add(exception);
	}
}
