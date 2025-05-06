package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddModifierSealedClassStrategy extends AddModifierClassStrategy {
	public AddModifierSealedClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(Modifier.SEALED, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tp.isFinal()) throw new ImpossibleChangeException();
		if (tp.isNonSealed()) throw new ImpossibleChangeException();

		var otherTypes = getAllOtherMutableTypes(mutableApi);
		var hasSubclasses = otherTypes.stream().anyMatch(t -> {
			if (t instanceof ClassBuilder classBuilder) {
				return classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName());
			}

			return false;
		});
		if (!hasSubclasses) throw new ImpossibleChangeException();

		ClassBuilder mutableClass = getMutableType(mutableApi);

		super.applyBreakToMutableApi(mutableApi);

		otherTypes.forEach(typeBuilder -> {
			if (typeBuilder instanceof ClassBuilder classBuilder) {
				if (classBuilder.superClass != null && classBuilder.superClass.getQualifiedName().equals(tp.getQualifiedName())) {
					mutableClass.permittedTypes.add(typeBuilder.qualifiedName);

					if (!typeBuilder.modifiers.contains(Modifier.FINAL) && !typeBuilder.modifiers.contains(Modifier.NON_SEALED)) {
						typeBuilder.modifiers.add(Modifier.NON_SEALED);
					}
				}
			}
		});
	}
}
