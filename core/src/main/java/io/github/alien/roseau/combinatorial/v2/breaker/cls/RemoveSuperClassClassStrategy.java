package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AbstractTpStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveSuperClassClassStrategy extends AbstractTpStrategy<ClassDecl> {
	public RemoveSuperClassClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue, "RemoveSuperClassFromClass%s".formatted(cls.getSimpleName()));
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.getSuperClass().getQualifiedName().equals("java.lang.Object")) throw new ImpossibleChangeException();

		var mutableClass = getMutableClass(mutableApi);
		mutableClass.superClass = mutableApi.typeReferenceFactory.createTypeReference("java.lang.Object");

		var mutableSuperType = mutableApi.allTypes.getOrDefault(tp.getSuperClass().getQualifiedName(), null);
		if (mutableSuperType instanceof ClassBuilder mutableSuperClass) {
			mutableSuperClass.permittedTypes.remove(tp.getQualifiedName());
		}
	}
}
