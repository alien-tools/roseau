package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.HashSet;
import java.util.List;

abstract class AbstractMtdStrategy extends AbstractTpMbrStrategy<MethodDecl> {
	AbstractMtdStrategy(MethodDecl mtd, NewApiQueue queue, String strategyName) {
		super(mtd, queue, strategyName);
	}

	protected MethodBuilder getMethodFrom(TypeBuilder containingType) {
		var method = containingType.methods.stream().filter(m -> m.make().equals(tpMbr)).findFirst();
		if (method.isEmpty()) throw new RuntimeException();

		return method.get();
	}

	protected MethodBuilder getMethodFrom(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);

		return getMethodFrom(containingType);
	}

	protected static boolean areMethodsInvalid(List<MethodBuilder> methods) {
		var erasureList = methods.stream().map(c -> c.make().getErasure()).toList();
		var erasureSet = new HashSet<>(erasureList);

		return erasureSet.size() != erasureList.size();
	}
}
