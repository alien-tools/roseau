package io.github.alien.roseau.combinatorial.v2.breaker.tpDcl;

import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddModifierTypeStrategy extends AbstractTpStrategy {
	protected final Modifier modifier;

	public AddModifierTypeStrategy(Modifier modifier, TypeDecl tp, NewApiQueue queue) {
		super(tp, queue, "Add%sModifierTo%s".formatted(modifier.toCapitalize(), tp.getSimpleName()));

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Adding {} modifier to {}", modifier, tp.getSimpleName());

		var mutableClass = mutableApi.allTypes.get(tp.getQualifiedName());
		if (mutableClass == null) throw new ImpossibleChangeException();

		mutableClass.modifiers.add(modifier);
	}
}
