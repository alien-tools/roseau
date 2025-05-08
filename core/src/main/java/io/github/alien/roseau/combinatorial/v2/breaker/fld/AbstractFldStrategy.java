package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.FieldBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractFldStrategy extends AbstractTpMbrStrategy<FieldDecl> {
	AbstractFldStrategy(FieldDecl fld, NewApiQueue queue, String strategyName, API api) {
		super(fld, queue, strategyName, api);
	}

	protected FieldBuilder getFieldFrom(TypeBuilder containingType) throws ImpossibleChangeException {
		var field = containingType.fields.stream().filter(m -> m.make().equals(tpMbr)).findFirst();
		if (field.isEmpty()) throw new ImpossibleChangeException();

		return field.get();
	}

	protected FieldBuilder getFieldFrom(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingTypeFromMutableApi(mutableApi);

		return getFieldFrom(containingType);
	}
}
