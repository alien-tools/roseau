package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddMethodTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddMethodAbstractClassStrategy extends AddMethodTypeStrategy<ClassDecl> {
	public AddMethodAbstractClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!tp.isAbstract()) throw new ImpossibleChangeException();

		super.applyBreakToMutableApi(mutableApi);

		var mutableClass = getMutableClass(mutableApi);
		var addedMethodBuilder = mutableClass.methods.getLast();

		getAllOtherMutableTypes(mutableApi).forEach(typeBuilder -> {
			if (typeBuilder instanceof ClassBuilder classBuilder) {
				if (classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName())) {
					if (!classBuilder.modifiers.contains(Modifier.ABSTRACT)) {
						var newMethodBuilder = MethodBuilder.from(addedMethodBuilder.make());
						newMethodBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(classBuilder.qualifiedName);
						newMethodBuilder.modifiers.remove(Modifier.ABSTRACT);
						classBuilder.methods.add(newMethodBuilder);
					}
				}
			}
		});
	}
}
