package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AbstractTpStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractRcdStrategy extends AbstractTpStrategy<RecordDecl> {
	AbstractRcdStrategy(RecordDecl rcd, NewApiQueue queue, String strategyName) {
		super(rcd, queue, strategyName);
	}

	protected RecordBuilder getMutableBuilderFromMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(tp.getQualifiedName());
		if (containingType == null) throw new RuntimeException();

		if (containingType instanceof RecordBuilder recordBuilder) return recordBuilder;

		throw new RuntimeException();
	}
}
