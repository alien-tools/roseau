package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeTypeMethodStrategy extends AbstractMtdStrategy {
	private final ITypeReference type;

	public ChangeTypeMethodStrategy(ITypeReference type, MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "ChangeMethod%sIn%sTypeTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				StringUtils.getPrettyQualifiedName(mtd.getContainingType()),
				StringUtils.getPrettyQualifiedName(type)),
			api
		);

		this.type = type;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getType().equals(type)) throw new ImpossibleChangeException();

		LOGGER.info("Changing method {} type to {}", tpMbr.getQualifiedName(), StringUtils.getPrettyQualifiedName(type));

		var method = getMethodFrom(mutableApi);
		method.type = type;
	}
}
