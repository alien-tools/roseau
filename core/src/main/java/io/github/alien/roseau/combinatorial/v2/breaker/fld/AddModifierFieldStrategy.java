package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierFieldStrategy extends AbstractFldStrategy {
	private final Modifier modifier;

	public AddModifierFieldStrategy(Modifier modifier, FieldDecl fld, NewApiQueue queue) {
		super(fld, queue, "AddModifier%sToField%sIn%s".formatted(
				modifier.toCapitalize(),
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName())
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Adding modifier {} to field {}", modifier.toCapitalize(), tpMbr.getQualifiedName());

		var field = getFieldFrom(mutableApi);
		field.modifiers.add(modifier);
	}
}
