package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

abstract class AddModifierClassStrategy extends AddModifierTypeStrategy<ClassDecl> {
	AddModifierClassStrategy(Modifier modifier, ClassDecl cls, NewApiQueue queue) {
		super(modifier, cls, queue);
	}
}
