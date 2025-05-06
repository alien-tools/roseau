package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierFinalClassStrategy extends AddModifierClassStrategy {
	public AddModifierFinalClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.FINAL, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.isAbstract()) throw new ImpossibleChangeException();
		if (tp.isNonSealed()) throw new ImpossibleChangeException();
		if (tp.isSealed()) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);

		getAllOtherMutableTypes(mutableApi).forEach(typeBuilder -> {
			if (typeBuilder instanceof ClassBuilder classBuilder) {
				if (classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName())) {
					classBuilder.superClass = null;
				}
			}
		});
	}
}
