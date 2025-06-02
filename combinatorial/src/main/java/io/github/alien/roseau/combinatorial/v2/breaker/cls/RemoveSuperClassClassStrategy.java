package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AbstractTpStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveSuperClassClassStrategy extends AbstractTpStrategy<ClassDecl> {
	public RemoveSuperClassClassStrategy(ClassDecl cls, NewApiQueue queue, API api) {
		super(cls, queue, "RemoveSuperClassFromClass%s".formatted(cls.getSimpleName()), api);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tp.getSuperClass().getQualifiedName().equals("java.lang.Object")) throw new ImpossibleChangeException();

		LOGGER.info("Removing super class from class {}", tp.getQualifiedName());

		var mutableClass = getMutableClass(mutableApi);
		mutableClass.superClass = mutableApi.typeReferenceFactory.createTypeReference("java.lang.Object");
	}
}
