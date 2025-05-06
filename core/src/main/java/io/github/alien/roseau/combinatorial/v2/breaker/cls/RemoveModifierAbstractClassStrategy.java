package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.MethodBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.ArrayList;
import java.util.stream.Collectors;

public final class RemoveModifierAbstractClassStrategy extends RemoveModifierClassStrategy {
	public RemoveModifierAbstractClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.ABSTRACT, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		super.applyBreakToMutableApi(mutableApi);

		var mutableClass = getMutableClass(mutableApi);
		mutableClass.methods = mutableClass.methods.stream()
				.peek(methodBuilder -> methodBuilder.modifiers.remove(Modifier.ABSTRACT))
				.collect(Collectors.toCollection(ArrayList::new));

		if (mutableClass.superClass != null && mutableClass.superClass.getResolvedApiType().isPresent()) {
			var superClass = mutableClass.superClass.getResolvedApiType().get();

			addMethodsToImplementFromTypeDeclToClassBuilder(superClass, mutableClass, mutableApi.typeReferenceFactory);
		}

		mutableClass.implementedInterfaces.forEach(interfaceTR -> {
			if (interfaceTR.getResolvedApiType().isEmpty()) return;
			var interfaceDecl = interfaceTR.getResolvedApiType().get();

			addMethodsToImplementFromTypeDeclToClassBuilder(interfaceDecl, mutableClass, mutableApi.typeReferenceFactory);
		});
	}

	private void addMethodsToImplementFromTypeDeclToClassBuilder(TypeDecl typeDecl, ClassBuilder mutableClass, TypeReferenceFactory factory) {
		typeDecl.getAllMethodsToImplement().forEach(mDecl -> {
			MethodBuilder mBuilder = MethodBuilder.from(mDecl);
			mBuilder.containingType = factory.createTypeReference(mutableClass.qualifiedName);
			mBuilder.modifiers.remove(Modifier.ABSTRACT);

			mutableClass.methods.add(mBuilder);
		});
	}
}
