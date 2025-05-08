package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveModifierTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	protected final Modifier modifier;

	public RemoveModifierTypeStrategy(Modifier modifier, T tp, NewApiQueue queue, API api) {
		super(tp, queue, "Remove%sModifierIn%s".formatted(modifier.toCapitalize(), tp.getSimpleName()), api);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Removing {} modifier from {}", modifier.toCapitalize(), tp.getSimpleName());

		var mutableType = mutableApi.allTypes.get(tp.getQualifiedName());
		if (mutableType == null) throw new ImpossibleChangeException();

		mutableType.modifiers.remove(modifier);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
