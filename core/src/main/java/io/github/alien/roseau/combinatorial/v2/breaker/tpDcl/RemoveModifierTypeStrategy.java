package io.github.alien.roseau.combinatorial.v2.breaker.tpDcl;

import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveModifierTypeStrategy extends AbstractTpStrategy {
	protected final Modifier modifier;

	public RemoveModifierTypeStrategy(Modifier modifier, TypeDecl tp, NewApiQueue queue) {
		super(tp, queue, "Remove%sModifierFrom%s".formatted(modifier.toCapitalize(), tp.getSimpleName()));

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Removing {} modifier from {}", modifier.toCapitalize(), tp.getSimpleName());

		var mutableClass = mutableApi.allTypes.get(tp.getQualifiedName());
		if (mutableClass == null) throw new ImpossibleChangeException();

		mutableClass.modifiers.remove(modifier);
	}
}
