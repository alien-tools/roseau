package io.github.alien.roseau.combinatorial.v2.breaker.intf;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.RemoveModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveModifierSealedInterfaceStrategy extends RemoveModifierTypeStrategy<InterfaceDecl> {
	public RemoveModifierSealedInterfaceStrategy(InterfaceDecl intf, NewApiQueue queue) {
		super(Modifier.SEALED, intf, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		super.applyBreakToMutableApi(mutableApi);

		var mutableInterface = getMutableInterface(mutableApi);
		mutableInterface.permittedTypes.clear();

		getAllOtherMutableTypes(mutableApi).forEach(typeBuilder -> {
			if (typeBuilder.modifiers.contains(Modifier.NON_SEALED)) {
				if (typeBuilder.implementedInterfaces.stream().anyMatch(i -> i.getQualifiedName().equals(tp.getQualifiedName()))) {
					var hasSealedSuperClass = (
							typeBuilder instanceof ClassBuilder classBuilder
									&& classBuilder.superClass != null
									&& classBuilder.superClass.getResolvedApiType().map(ClassDecl::isSealed).orElse(false)
					);
					var hasOtherSealedImplementedInterface = typeBuilder.implementedInterfaces.stream().anyMatch(i ->
							!i.getQualifiedName().equals(tp.getQualifiedName())
									&& i.getResolvedApiType().map(InterfaceDecl::isSealed).orElse(false)
					);

					if (!hasSealedSuperClass && !hasOtherSealedImplementedInterface) {
						typeBuilder.modifiers.remove(Modifier.NON_SEALED);
					}
				}
			}
		});
	}
}
