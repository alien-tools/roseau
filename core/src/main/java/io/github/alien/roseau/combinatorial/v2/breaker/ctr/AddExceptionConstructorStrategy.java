package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddExceptionConstructorStrategy extends AbstractCtrStrategy {
	private final ITypeReference exception;

	public AddExceptionConstructorStrategy(ITypeReference exception, ConstructorDecl ctr, NewApiQueue queue) {
		super(ctr, queue, "AddException%sToConstructor%sIn%s".formatted(
				exception.getPrettyQualifiedName(),
				StringUtils.splitSpecialCharsAndCapitalize(ctr.getErasure()),
				ctr.getContainingType().getPrettyQualifiedName())
		);

		this.exception = exception;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tpMbr.getThrownExceptions().contains(exception)) throw new ImpossibleChangeException();

		var constructor = getConstructorFrom(mutableApi);

		LOGGER.info("Adding exception {} to constructor {}", exception.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		constructor.thrownExceptions.add(exception);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
