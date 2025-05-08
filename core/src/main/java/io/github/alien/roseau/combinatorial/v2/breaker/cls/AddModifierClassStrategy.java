package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierClassStrategy extends AddModifierTypeStrategy<ClassDecl> {
	public AddModifierClassStrategy(Modifier modifier, ClassDecl cls, NewApiQueue queue, API api) {
		super(modifier, cls, queue, api);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.getModifiers().contains(Modifier.ABSTRACT) && modifier.equals(Modifier.FINAL)) throw new ImpossibleChangeException();
		if (tp.getModifiers().contains(Modifier.FINAL) && modifier.equals(Modifier.ABSTRACT)) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
