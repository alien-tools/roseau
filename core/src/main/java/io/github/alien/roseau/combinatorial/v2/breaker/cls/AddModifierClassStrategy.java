package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddModifierClassStrategy extends AbstractClsStrategy {
	private final Modifier modifier;

	public AddModifierClassStrategy(Modifier modifier, ClassDecl cls, NewApiQueue queue) {
		super(cls, queue, "Add%sModifierTo%s".formatted(modifier, cls.getSimpleName()));

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (cls.getModifiers().contains(modifier)) throw new ImpossibleChangeException();
		if (cls.getModifiers().contains(Modifier.ABSTRACT) && modifier.equals(Modifier.FINAL)) throw new ImpossibleChangeException();
		if (cls.getModifiers().contains(Modifier.FINAL) && modifier.equals(Modifier.ABSTRACT)) throw new ImpossibleChangeException();

		LOGGER.info("Adding {} modifier to {}", modifier, cls.getSimpleName());

		var mutableClass = mutableApi.allTypes.get(cls.getQualifiedName());
		if (mutableClass == null) throw new ImpossibleChangeException();

		mutableClass.modifiers.add(modifier);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
