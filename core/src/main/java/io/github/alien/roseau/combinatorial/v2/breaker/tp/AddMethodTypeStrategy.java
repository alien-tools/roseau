package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.reference.PrimitiveTypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public class AddMethodTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	public AddMethodTypeStrategy(T tp, NewApiQueue queue) {
		super(tp, queue, "AddMethodToType%s".formatted(tp.getSimpleName()));
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		LOGGER.info("Adding new method to type {}", tp.getSimpleName());

		var methodBuilder = new MethodBuilder();
		methodBuilder.qualifiedName = "%s.%s".formatted(tp.getQualifiedName(), "newMethodAddedToType");
		methodBuilder.visibility = AccessModifier.PUBLIC;
		methodBuilder.modifiers.add(Modifier.ABSTRACT);
		methodBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(tp.getQualifiedName());
		methodBuilder.type = new PrimitiveTypeReference("void");

		var mutableType = getMutableType(mutableApi);
		mutableType.methods.add(methodBuilder);

		getAllOtherMutableTypes(mutableApi).forEach(typeBuilder -> {
			if (typeBuilder instanceof ClassBuilder classBuilder) {
				var hasCurrentTypeAsSuperClass = classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName());
				var hasCurrentTypeAsImplementedInterface = classBuilder.implementedInterfaces.stream().anyMatch(i -> i.getQualifiedName().equals(tp.getQualifiedName()));

				if (hasCurrentTypeAsSuperClass || hasCurrentTypeAsImplementedInterface) {
					if (!classBuilder.modifiers.contains(Modifier.ABSTRACT)) {
						var newMethodBuilder = MethodBuilder.from(methodBuilder.make());
						newMethodBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(classBuilder.qualifiedName);
						newMethodBuilder.modifiers.remove(Modifier.ABSTRACT);
						classBuilder.methods.add(newMethodBuilder);
					}
				}
			}
		});
	}
}
