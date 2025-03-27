package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveFieldStrategy extends AbstractFldStrategy {
	public RemoveFieldStrategy(FieldDecl fld, NewApiQueue queue) {
		super(fld, queue, "RemoveField%sFrom%s".formatted(
				StringUtils.capitalizeFirstLetter(fld.getSimpleName()),
				fld.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(fld.getContainingType().getQualifiedName());
		if (containingType == null) throw new ImpossibleChangeException();

		LOGGER.info("Removing field {} from {}", fld.getPrettyQualifiedName(), containingType.qualifiedName);

		containingType.fields.remove(fld);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
