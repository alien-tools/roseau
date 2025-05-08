package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveMethodStrategy extends AbstractMtdStrategy {
	public RemoveMethodStrategy(MethodDecl mtd, NewApiQueue queue, API api) {
		super(mtd, queue, "RemoveMethod%sIn%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(api.getErasure(mtd)),
				mtd.getContainingType().getPrettyQualifiedName()),
				api
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);

		LOGGER.info("Removing method {} from {}", tpMbr.getPrettyQualifiedName(), containingType.qualifiedName);

		containingType.methods = containingType.methods.stream().filter(m -> !m.make().equals(tpMbr)).toList();

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
