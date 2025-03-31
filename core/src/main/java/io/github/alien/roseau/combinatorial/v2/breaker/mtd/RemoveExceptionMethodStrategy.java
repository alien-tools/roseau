package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveExceptionMethodStrategy extends AbstractMtdStrategy {
	private final ITypeReference exception;

	public RemoveExceptionMethodStrategy(ITypeReference exception, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "RemoveException%sFromMethod%sIn%s".formatted(
				exception.getPrettyQualifiedName(),
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.exception = exception;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!mtd.getThrownExceptions().contains(exception)) throw new ImpossibleChangeException();

		var method = getMethodFrom(mutableApi);

		LOGGER.info("Removing exception {} from method {}", exception.getPrettyQualifiedName(), mtd.getQualifiedName());

		method.thrownExceptions.remove(exception);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
