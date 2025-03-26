package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddMethodAbstractClassStrategy extends AbstractClsStrategy {
	public AddMethodAbstractClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue, "AddMethodToAbstractClass%s".formatted(cls.getSimpleName()));
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (!cls.getModifiers().contains(Modifier.ABSTRACT)) throw new ImpossibleChangeException();

		LOGGER.info("Adding new method to abstract class {}", cls.getSimpleName());

		var mutableClass = mutableApi.allTypes.get(cls.getQualifiedName());
		if (mutableClass == null) throw new ImpossibleChangeException();

		var methodBuilder = new MethodBuilder();
		methodBuilder.qualifiedName = "%s.%s".formatted(cls.getQualifiedName(), "newMethodAddedToAbstractClass");
		methodBuilder.visibility = AccessModifier.PUBLIC;
		methodBuilder.modifiers.add(Modifier.ABSTRACT);
		methodBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(cls.getQualifiedName());
		methodBuilder.type = new PrimitiveTypeReference("void");

		mutableClass.methods.add(methodBuilder.make());
	}
}
