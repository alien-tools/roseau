package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveConstructorStrategy extends AbstractCtrStrategy {
	public RemoveConstructorStrategy(ConstructorDecl ctr, NewApiQueue queue) {
		super(ctr, queue, "RemoveConstructor%sFrom%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(ctr.getErasure()),
				ctr.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(ctr.getContainingType().getQualifiedName());
		if (containingType == null) throw new ImpossibleChangeException();

		if (containingType instanceof ClassBuilder classBuilder) {
			LOGGER.info("Removing constructor {} from {}", ctr.getPrettyQualifiedName(), containingType.qualifiedName);

			classBuilder.constructors.remove(ctr);

			// TODO: For now we don't have hierarchy, so we don't need to update possible references
		} else {
			throw new ImpossibleChangeException();
		}
	}
}
