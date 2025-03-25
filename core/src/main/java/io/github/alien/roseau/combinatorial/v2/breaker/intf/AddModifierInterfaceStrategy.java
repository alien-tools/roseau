package io.github.alien.roseau.combinatorial.v2.breaker.intf;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddModifierInterfaceStrategy extends AbstractIntfStrategy {
	private final Modifier modifier;

	public AddModifierInterfaceStrategy(Modifier modifier, InterfaceDecl intf, NewApiQueue queue) {
		super(intf, queue, "Add%sModifierTo%s".formatted(modifier.toCapitalize(), intf.getSimpleName()));

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (intf.getModifiers().contains(modifier)) throw new ImpossibleChangeException();

		LOGGER.info("Adding {} modifier to {}", modifier, intf.getSimpleName());

		var mutableClass = mutableApi.allTypes.get(intf.getQualifiedName());
		if (mutableClass == null) throw new ImpossibleChangeException();

		mutableClass.modifiers.add(modifier);
	}
}
