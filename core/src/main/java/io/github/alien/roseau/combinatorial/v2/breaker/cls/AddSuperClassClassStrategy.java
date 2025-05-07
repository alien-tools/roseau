package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AbstractTpStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddSuperClassClassStrategy extends AbstractTpStrategy<ClassDecl> {
	public AddSuperClassClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue, "AddSuperClassToClass%s".formatted(cls.getSimpleName()));
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.getSuperClass().getQualifiedName().equals("java.lang.Object")) throw new ImpossibleChangeException();

		var newClass = new ClassBuilder();
		newClass.qualifiedName = "api.NewClass";
		newClass.visibility = AccessModifier.PUBLIC;

		var mutableClass = getMutableClass(mutableApi);
		mutableApi.allTypes.put(newClass.qualifiedName, newClass);
		mutableClass.superClass = mutableApi.typeReferenceFactory.createTypeReference(newClass.qualifiedName);
	}
}
