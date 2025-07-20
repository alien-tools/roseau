package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddAbstractMethodTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	public AddAbstractMethodTypeStrategy(T tp, NewApiQueue queue, API api) {
		super(tp, queue, "AddMethodToType%s".formatted(tp.getSimpleName()), api);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Adding new abstract method to type {}", tp.getSimpleName());

		var methodBuilder = new MethodBuilder();
		methodBuilder.qualifiedName = "%s.%s".formatted(tp.getQualifiedName(), "newMethodAddedToType");
		methodBuilder.visibility = AccessModifier.PUBLIC;
		methodBuilder.modifiers.add(Modifier.ABSTRACT);
		methodBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(tp.getQualifiedName());
		methodBuilder.type = new PrimitiveTypeReference("void");

		var mutableType = getMutableType(mutableApi);
		mutableType.methods.add(methodBuilder);
	}
}
