package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractMtdStrategy extends AbstractTpMbrStrategy<MethodDecl> {
	AbstractMtdStrategy(MethodDecl mtd, NewApiQueue queue, String strategyName, API api) {
		super(mtd, queue, strategyName, api);
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
}
