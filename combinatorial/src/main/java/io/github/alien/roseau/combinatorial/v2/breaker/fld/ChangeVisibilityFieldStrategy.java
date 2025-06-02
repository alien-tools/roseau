package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityFieldStrategy extends AbstractFldStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityFieldStrategy(AccessModifier modifier, FieldDecl fld, NewApiQueue queue, API api) {
		super(fld, queue, "ReduceField%sIn%sVisibilityTo%s".formatted(
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName(),
				modifier.toCapitalize()),
				api
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		LOGGER.info("Reducing field {} visibility to {}", tpMbr.getQualifiedName(), accessModifier.toCapitalize());

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		var field = this.getFieldFrom(containingType);
		field.visibility = accessModifier;
	}
}
