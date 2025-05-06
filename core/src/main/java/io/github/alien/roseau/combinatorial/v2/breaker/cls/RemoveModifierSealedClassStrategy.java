package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierSealedClassStrategy extends RemoveModifierClassStrategy {
	public RemoveModifierSealedClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.SEALED, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		super.applyBreakToMutableApi(mutableApi);

		ClassBuilder mutableClass = getMutableType(mutableApi);
		mutableClass.permittedTypes.clear();

		getAllOtherMutableTypes(mutableApi).forEach(typeBuilder -> {
			if (typeBuilder.modifiers.contains(Modifier.NON_SEALED)) {
				if (typeBuilder instanceof ClassBuilder classBuilder) {
					if (classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName())) {
						if (classBuilder.implementedInterfaces.stream().noneMatch(tR -> tR.getResolvedApiType().map(TypeDecl::isSealed).orElse(false))) {
							classBuilder.modifiers.remove(Modifier.NON_SEALED);
						}
					}
				}
			}
		});
	}
}
