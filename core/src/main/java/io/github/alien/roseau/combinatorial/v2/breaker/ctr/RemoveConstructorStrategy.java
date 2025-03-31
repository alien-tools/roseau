package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveConstructorStrategy extends AbstractCtrStrategy {
	public RemoveConstructorStrategy(ConstructorDecl ctr, NewApiQueue queue) {
		super(ctr, queue, "RemoveConstructor%sIn%s".formatted(
				StringUtils.splitSpecialCharsAndCapitalize(ctr.getErasure()),
				ctr.getContainingType().getPrettyQualifiedName())
		);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		var containingClass = getContainingClassFromMutableApi(mutableApi);

		containingClass.constructors = containingClass.constructors.stream().filter(c -> !c.make().equals(tpMbr)).toList();

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
