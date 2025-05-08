package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveExceptionConstructorStrategy extends AbstractCtrStrategy {
	private final ITypeReference exception;

	public RemoveExceptionConstructorStrategy(ITypeReference exception, ConstructorDecl ctr, NewApiQueue queue, API api) {
		super(ctr, queue, "RemoveException%sFromConstructor%sIn%s".formatted(
				exception.getPrettyQualifiedName(),
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(ctr)),
				ctr.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.exception = exception;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tpMbr.getThrownExceptions().contains(exception)) throw new ImpossibleChangeException();

		var constructor = getConstructorFrom(mutableApi);

		LOGGER.info("Removing exception {} from constructor {}", exception.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		constructor.thrownExceptions.remove(exception);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
