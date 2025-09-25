package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierFieldStrategy extends AbstractFldStrategy {
	private final Modifier modifier;

	public RemoveModifierFieldStrategy(Modifier modifier, FieldDecl fld, NewApiQueue queue, API api) {
		super(fld, queue, "RemoveModifier%sToField%sIn%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(modifier.name()),
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				StringUtils.getPrettyQualifiedName(fld.getContainingType())),
			api
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (!tpMbr.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Removing modifier {} to field {}", StringUtils.splitSpecialCharsAndCapitalize(modifier.name()), tpMbr.getQualifiedName());

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		var field = getFieldFrom(containingType);
		field.modifiers.remove(modifier);
	}
}
