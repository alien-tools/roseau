package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddMethodTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddMethodAbstractClassStrategy extends AddMethodTypeStrategy<ClassDecl> {
	public AddMethodAbstractClassStrategy(ClassDecl cls, NewApiQueue queue, API api) {
		super(cls, queue, api);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.getModifiers().contains(Modifier.ABSTRACT)) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);
	}
}
