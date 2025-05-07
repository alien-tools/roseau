package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierNonSealedClassStrategy extends AddModifierClassStrategy {
	public AddModifierNonSealedClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.NON_SEALED, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.isFinal()) throw new ImpossibleChangeException();

		var hasSealedSuperClass = tp.getSuperClass().getResolvedApiType().map(ClassDecl::isSealed).orElse(false);
		var hasAtLeastOneSealedImplementedInterface = tp.getImplementedInterfaces().stream()
				.anyMatch(i -> i.getResolvedApiType().map(InterfaceDecl::isSealed).orElse(false));

		if (!hasSealedSuperClass && !hasAtLeastOneSealedImplementedInterface) throw new ImpossibleChangeException();

		// Only possible case to add non-sealed modifier is to remove final modifier
		var mutableClass = getMutableClass(mutableApi);
		mutableClass.modifiers.remove(Modifier.FINAL);

		super.applyBreakToMutableApi(mutableApi);
	}
}
