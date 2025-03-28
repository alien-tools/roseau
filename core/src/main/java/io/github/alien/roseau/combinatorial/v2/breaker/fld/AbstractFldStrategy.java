package io.github.alien.roseau.combinatorial.v2.breaker.fld;

import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.FieldBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tpMbr.AbstractTpMbrStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AbstractFldStrategy extends AbstractTpMbrStrategy {
	protected final FieldDecl fld;

	AbstractFldStrategy(FieldDecl fld, NewApiQueue queue, String strategyName) {
		super(fld, queue, strategyName);

		this.fld = fld;
	}

	protected FieldBuilder getFieldFrom(TypeBuilder containingType) throws ImpossibleChangeException {
		var field = containingType.fields.stream().filter(m -> m.make().equals(fld)).findFirst();
		if (field.isEmpty()) throw new ImpossibleChangeException();

		return field.get();
	}

	protected FieldBuilder getFieldFrom(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var containingType = getContainingTypeFromMutableApi(mutableApi);

		return getFieldFrom(containingType);
	}
}
