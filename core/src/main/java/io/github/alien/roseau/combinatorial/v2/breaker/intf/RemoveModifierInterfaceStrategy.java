package io.github.alien.roseau.combinatorial.v2.breaker.intf;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class RemoveModifierInterfaceStrategy extends AbstractIntfStrategy {
	private final Modifier modifier;

	public RemoveModifierInterfaceStrategy(Modifier modifier, InterfaceDecl intf, NewApiQueue queue) {
		super(intf, queue, "Remove%sModifierFrom%s".formatted(modifier.toCapitalize(), intf.getSimpleName()));

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!intf.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Removing {} modifier from {}", modifier.toCapitalize(), intf.getSimpleName());

		var mutableClass = mutableApi.allTypes.get(intf.getQualifiedName());
		if (mutableClass == null) throw new ImpossibleChangeException();

		mutableClass.modifiers.remove(modifier);
	}
}
