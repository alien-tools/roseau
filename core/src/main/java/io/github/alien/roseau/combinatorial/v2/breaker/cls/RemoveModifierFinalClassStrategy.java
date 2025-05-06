package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierFinalClassStrategy extends RemoveModifierClassStrategy {
	public RemoveModifierFinalClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.FINAL, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.getSuperClass().getResolvedApiType().map(TypeDecl::isSealed).orElse(false))
			throw new ImpossibleChangeException();
		if (tp.getImplementedInterfaces().stream().anyMatch(i -> i.getResolvedApiType().map(TypeDecl::isSealed).orElse(false)))
			throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);
	}
}
