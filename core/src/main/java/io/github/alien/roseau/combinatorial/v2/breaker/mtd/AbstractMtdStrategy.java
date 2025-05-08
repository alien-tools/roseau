package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.HashSet;
import java.util.List;

abstract class AbstractMtdStrategy extends AbstractTpMbrStrategy<MethodDecl> {
	AbstractMtdStrategy(MethodDecl mtd, NewApiQueue queue, String strategyName, API api) {
		super(mtd, queue, strategyName, api);
	}

	protected MethodBuilder getMethodFrom(TypeBuilder containingType) throws ImpossibleChangeException {
		var method = containingType.methods.stream().filter(m -> m.make().equals(tpMbr)).findFirst();
		if (method.isEmpty()) throw new ImpossibleChangeException();

		return method.get();
	}

	protected MethodBuilder getMethodFrom(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingTypeFromMutableApi(mutableApi);

		return getMethodFrom(containingType);
	}

	protected static boolean areMethodsInvalid(List<MethodBuilder> methods, API api) {
		var erasureList = methods.stream().map(c -> api.getErasure(c.make())).toList();
		var erasureSet = new HashSet<>(erasureList);

		return erasureSet.size() != erasureList.size();
	}
}
