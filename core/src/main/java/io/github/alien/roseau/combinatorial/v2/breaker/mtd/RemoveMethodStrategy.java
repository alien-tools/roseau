package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveMethodStrategy extends AbstractMtdStrategy {
	public RemoveMethodStrategy(MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "RemoveMethod%sFrom%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(mtd.getContainingType().getQualifiedName());
		if (containingType == null) throw new ImpossibleChangeException();

		LOGGER.info("Removing method {} from {}", mtd.getPrettyQualifiedName(), containingType.qualifiedName);

		containingType.methods.remove(mtd);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
