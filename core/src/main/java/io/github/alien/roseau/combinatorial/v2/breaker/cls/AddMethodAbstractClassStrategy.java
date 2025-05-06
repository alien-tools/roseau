package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddMethodTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddMethodAbstractClassStrategy extends AddMethodTypeStrategy<ClassDecl> {
	public AddMethodAbstractClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.isAbstract()) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);
	}
}
