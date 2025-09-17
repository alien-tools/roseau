package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveFieldStrategy extends AbstractFldStrategy {
	public RemoveFieldStrategy(FieldDecl fld, NewApiQueue queue, API api) {
		super(fld, queue, "RemoveField%sIn%s".formatted(
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName()),
				api
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);
		LOGGER.info("Removing field {} from {}", tpMbr.getPrettyQualifiedName(), containingType.qualifiedName);

		containingType.fields = containingType.fields.stream().filter(f -> !f.make().equals(tpMbr)).toList();
	}
}
