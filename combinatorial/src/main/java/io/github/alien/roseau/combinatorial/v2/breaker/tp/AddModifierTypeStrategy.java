package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.utils.StringUtils;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	private final Modifier modifier;

	public AddModifierTypeStrategy(Modifier modifier, T tp, NewApiQueue queue, API api) {
		super(tp, queue, "Add%sModifierTo%s".formatted(StringUtils.splitSpecialCharsAndCapitalize(modifier.name()), tp.getSimpleName()), api);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tp.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Adding {} modifier to {}", modifier, tp.getSimpleName());

		var mutableType = mutableApi.allTypes.get(tp.getQualifiedName());
		mutableType.modifiers.add(modifier);
	}
}
