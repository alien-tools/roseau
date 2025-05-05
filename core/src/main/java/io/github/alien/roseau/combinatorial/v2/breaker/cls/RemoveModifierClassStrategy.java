package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.RemoveModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class RemoveModifierClassStrategy extends RemoveModifierTypeStrategy<ClassDecl> {
	RemoveModifierClassStrategy(Modifier modifier, ClassDecl cls, NewApiQueue queue) {
		super(modifier, cls, queue);
	}
}
