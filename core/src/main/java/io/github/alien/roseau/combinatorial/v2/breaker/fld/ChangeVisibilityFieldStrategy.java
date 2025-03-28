package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeVisibilityFieldStrategy extends AbstractFldStrategy {
	private final AccessModifier accessModifier;

	public ChangeVisibilityFieldStrategy(AccessModifier modifier, FieldDecl fld, NewApiQueue queue) {
		super(fld, queue, "ReduceField%sFrom%sVisibilityTo%s".formatted(
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName(),
				modifier.toCapitalize())
		);

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (fld.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof InterfaceBuilder && (accessModifier == AccessModifier.PRIVATE || accessModifier == AccessModifier.PROTECTED))
			throw new ImpossibleChangeException();

		var field = this.getFieldFrom(containingType);

		LOGGER.info("Reducing field {} visibility to {}", fld.getQualifiedName(), accessModifier.toCapitalize());

		field.visibility = accessModifier;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
