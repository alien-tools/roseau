package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierFieldStrategy extends AbstractFldStrategy {
	private final Modifier modifier;

	public RemoveModifierFieldStrategy(Modifier modifier, FieldDecl fld, NewApiQueue queue, API api) {
		super(fld, queue, "RemoveModifier%sToField%sIn%s".formatted(
				modifier.toCapitalize(),
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName()),
				api
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tpMbr.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof RecordBuilder && modifier == Modifier.STATIC) throw new ImpossibleChangeException();

		var field = getFieldFrom(containingType);

		LOGGER.info("Removing modifier {} to field {}", modifier.toCapitalize(), tpMbr.getQualifiedName());

		field.modifiers.remove(modifier);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
