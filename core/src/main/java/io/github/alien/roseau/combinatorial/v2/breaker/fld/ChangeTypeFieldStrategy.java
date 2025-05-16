package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeTypeFieldStrategy extends AbstractFldStrategy {
	private final ITypeReference type;

	public ChangeTypeFieldStrategy(ITypeReference type, FieldDecl fld, NewApiQueue queue) {
		super(fld, queue, "ChangeField%sIn%sTypeTo%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName(),
				type.getPrettyQualifiedName())
		);

		this.type = type;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tpMbr.getType().equals(type)) throw new ImpossibleChangeException();

		LOGGER.info("Changing field {} type to {}", tpMbr.getQualifiedName(), type.getPrettyQualifiedName());

		var field = getFieldFrom(mutableApi);
		field.type = type;
	}
}
