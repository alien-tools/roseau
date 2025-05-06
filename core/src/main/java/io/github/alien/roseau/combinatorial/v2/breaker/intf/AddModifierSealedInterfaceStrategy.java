package io.github.alien.roseau.combinatorial.v2.breaker.intf;

import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.EnumBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierSealedInterfaceStrategy extends AddModifierTypeStrategy<InterfaceDecl> {
	public AddModifierSealedInterfaceStrategy(InterfaceDecl intf, NewApiQueue queue) {
		super(Modifier.SEALED, intf, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.isNonSealed()) throw new ImpossibleChangeException();

		var subtypes = getAllOtherMutableTypes(mutableApi).stream()
				.filter(t -> t.implementedInterfaces.stream().anyMatch(i -> i.getQualifiedName().equals(tp.getQualifiedName())))
				.toList();
		if (subtypes.isEmpty()) throw new ImpossibleChangeException();

		var mutableInterface = getMutableInterface(mutableApi);

		super.applyBreakToMutableApi(mutableApi);

		subtypes.forEach(typeBuilder -> {
			mutableInterface.permittedTypes.add(typeBuilder.qualifiedName);

			if (
					!typeBuilder.modifiers.contains(Modifier.FINAL)
					&& !typeBuilder.modifiers.contains(Modifier.NON_SEALED)
					&& !(typeBuilder instanceof EnumBuilder)
					&& !(typeBuilder instanceof RecordBuilder)
			) {
				typeBuilder.modifiers.add(Modifier.NON_SEALED);
			}
		});
	}
}
