package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveImplementedInterfaceTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	private final InterfaceDecl implementedInterfaceDecl;

	public RemoveImplementedInterfaceTypeStrategy(InterfaceDecl implementedInterfaceDecl, T tp, NewApiQueue queue) {
		super(tp, queue, "RemoveImplementedInterface%sFromType%s".formatted(implementedInterfaceDecl.getPrettyQualifiedName(), tp.getSimpleName()));

		this.implementedInterfaceDecl = implementedInterfaceDecl;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var mutableType = getMutableType(mutableApi);
		mutableType.implementedInterfaces.removeIf(tR -> tR.getQualifiedName().equals(implementedInterfaceDecl.getQualifiedName()));

		if (tp.isNonSealed()) {
			var hasSealedSuperClass = tp instanceof ClassDecl classDecl && classDecl.getSuperClass().getResolvedApiType().map(ClassDecl::isSealed).orElse(false);
			var hasAtLeastOneSealedImplementInterface = mutableType.implementedInterfaces.stream().anyMatch(tR -> tR.getResolvedApiType().map(InterfaceDecl::isSealed).orElse(false));

			if (!hasSealedSuperClass && !hasAtLeastOneSealedImplementInterface) {
				mutableType.modifiers.remove(Modifier.NON_SEALED);
			}
		}

		var mutableImplementedType = mutableApi.allTypes.get(implementedInterfaceDecl.getQualifiedName());
		if (mutableImplementedType instanceof InterfaceBuilder mutableImplementedInterface) {
			mutableImplementedInterface.permittedTypes.remove(tp.getQualifiedName());
		}
	}
}
