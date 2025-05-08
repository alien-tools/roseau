package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeTypeFieldStrategy extends AbstractFldStrategy {
	private final ITypeReference type;

	public ChangeTypeFieldStrategy(ITypeReference type, FieldDecl fld, NewApiQueue queue, API api) {
		super(fld, queue, "ChangeField%sIn%sTypeTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName(),
				type.getPrettyQualifiedName()),
				api
		);

		this.type = type;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tpMbr.getType().equals(type)) throw new ImpossibleChangeException();

		var field = getFieldFrom(mutableApi);

		LOGGER.info("Changing field {} type to {}", tpMbr.getQualifiedName(), type.getPrettyQualifiedName());

		field.type = type;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
