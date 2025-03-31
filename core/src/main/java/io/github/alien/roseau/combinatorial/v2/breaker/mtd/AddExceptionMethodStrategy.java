package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddExceptionMethodStrategy extends AbstractMtdStrategy {
	private final ITypeReference exception;

	public AddExceptionMethodStrategy(ITypeReference exception, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "AddException%sToMethod%sIn%s".formatted(
				exception.getPrettyQualifiedName(),
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.exception = exception;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tpMbr.getThrownExceptions().contains(exception)) throw new ImpossibleChangeException();

		var method = getMethodFrom(mutableApi);

		LOGGER.info("Adding exception {} to method {}", exception.getPrettyQualifiedName(), tpMbr.getQualifiedName());

		method.thrownExceptions.add(exception);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
