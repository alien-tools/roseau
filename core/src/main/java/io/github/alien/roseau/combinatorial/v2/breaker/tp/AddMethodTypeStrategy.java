package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddMethodTypeStrategy extends AbstractTpStrategy {
	public AddMethodTypeStrategy(TypeDecl tp, NewApiQueue queue) {
		super(tp, queue, "AddMethodToType%s".formatted(tp.getSimpleName()));
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		LOGGER.info("Adding new method to type {}", tp.getSimpleName());

		var mutableType = mutableApi.allTypes.get(tp.getQualifiedName());
		if (mutableType == null) throw new ImpossibleChangeException();

		var methodBuilder = new MethodBuilder();
		methodBuilder.qualifiedName = "%s.%s".formatted(tp.getQualifiedName(), "newMethodAddedToType");
		methodBuilder.visibility = AccessModifier.PUBLIC;
		methodBuilder.modifiers.add(Modifier.ABSTRACT);
		methodBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(tp.getQualifiedName());
		methodBuilder.type = new PrimitiveTypeReference("void");

		mutableType.methods.add(methodBuilder);
	}
}
