package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddMethodTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddMethodAbstractClassStrategy extends AddMethodTypeStrategy {
	public AddMethodAbstractClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.getModifiers().contains(Modifier.ABSTRACT)) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);
	}
}
