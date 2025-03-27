package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.RemoveModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.stream.Collectors;

public final class RemoveModifierClassStrategy extends RemoveModifierTypeStrategy {
	public RemoveModifierClassStrategy(Modifier modifier, ClassDecl cls, NewApiQueue queue) {
		super(modifier, cls, queue);
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		super.applyBreakToMutableApi(mutableApi);

		if (modifier.equals(Modifier.ABSTRACT)) {
			var mutableClass = mutableApi.allTypes.get(tp.getQualifiedName());
			mutableClass.methods = mutableClass.methods.stream().map(m -> {
				if (!m.isAbstract()) return m;

				var methodFilteredModifiers = m.getModifiers().stream().filter(mod -> !mod.equals(Modifier.ABSTRACT)).collect(Collectors.toSet());
				return new MethodDecl(
						m.getQualifiedName(),
						m.getVisibility(),
						methodFilteredModifiers,
						m.getAnnotations(),
						m.getLocation(),
						m.getContainingType(),
						m.getType(),
						m.getParameters(),
						m.getFormalTypeParameters(),
						m.getThrownExceptions()
				);
			}).toList();
		}

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
