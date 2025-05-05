package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddAbstractModifierClassStrategy extends AddModifierClassStrategy {
	public AddAbstractModifierClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.ABSTRACT, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.isFinal()) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);
	}
}
