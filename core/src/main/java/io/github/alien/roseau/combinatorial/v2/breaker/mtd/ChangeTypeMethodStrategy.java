package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeTypeMethodStrategy extends AbstractMtdStrategy {
	private final ITypeReference type;

	public ChangeTypeMethodStrategy(ITypeReference type, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "ChangeMethod%sIn%sTypeTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName(),
				type.getPrettyQualifiedName())
		);

		this.type = type;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Changing method {} type to {}", tpMbr.getQualifiedName(), type.getPrettyQualifiedName());

		var method = getMethodFrom(mutableApi);
		method.type = type;
	}
}
