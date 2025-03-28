package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.ConstructorBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractCtrStrategy extends AbstractTpMbrStrategy {
	protected final ConstructorDecl ctr;

	AbstractCtrStrategy(ConstructorDecl ctr, NewApiQueue queue, String strategyName) {
		super(ctr, queue, strategyName);

		this.ctr = ctr;
	}

	protected ConstructorBuilder getConstructorFrom(ClassBuilder containingType) throws ImpossibleChangeException {
		var constructor = containingType.constructors.stream().filter(m -> m.make().equals(ctr)).findFirst();
		if (constructor.isEmpty()) throw new ImpossibleChangeException();

		return constructor.get();
	}

	protected ConstructorBuilder getConstructorFrom(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);

		return getConstructorFrom(containingType);
	}
}
