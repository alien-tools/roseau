package io.github.alien.roseau.combinatorial.v2.breaker.ctr;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.ConstructorBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.HashSet;
import java.util.List;

abstract class AbstractCtrStrategy extends AbstractTpMbrStrategy<ConstructorDecl> {
	AbstractCtrStrategy(ConstructorDecl ctr, NewApiQueue queue, String strategyName, API api) {
		super(ctr, queue, strategyName, api);
	}

	protected ConstructorBuilder getConstructorFrom(ClassBuilder containingType) throws ImpossibleChangeException {
		var constructor = containingType.constructors.stream().filter(m -> m.make().equals(tpMbr)).findFirst();
		if (constructor.isEmpty()) throw new ImpossibleChangeException();

		return constructor.get();
	}

	protected ConstructorBuilder getConstructorFrom(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingClassFromMutableApi(mutableApi);

		return getConstructorFrom(containingType);
	}

	protected static boolean areConstructorsInvalid(List<ConstructorBuilder> constructors, API api) {
		var erasureList = constructors.stream().map(c -> api.erasure().getErasure(c.make())).toList();
		var erasureSet = new HashSet<>(erasureList);

		return erasureSet.size() != erasureList.size();
	}
}
