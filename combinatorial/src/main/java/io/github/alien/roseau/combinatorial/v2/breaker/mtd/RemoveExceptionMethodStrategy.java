package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveExceptionMethodStrategy extends AbstractMtdStrategy {
	private final ITypeReference exception;

	public RemoveExceptionMethodStrategy(ITypeReference exception, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "RemoveException%sFromMethod%sIn%s".formatted(
				StringUtils.getPrettyQualifiedName(exception),
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				StringUtils.getPrettyQualifiedName(mtd.getContainingType())),
			api
		);

		this.exception = exception;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (!tpMbr.getThrownExceptions().contains(exception)) throw new ImpossibleChangeException();

		LOGGER.info("Removing exception {} from method {}", StringUtils.getPrettyQualifiedName(exception), tpMbr.getQualifiedName());

		var method = getMethodFrom(mutableApi);
		method.thrownExceptions.remove(exception);
	}
}
