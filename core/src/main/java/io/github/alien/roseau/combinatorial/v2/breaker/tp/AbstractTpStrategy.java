package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.builder.TypeBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.List;

public abstract class AbstractTpStrategy<T extends TypeDecl> extends AbstractApiBreakerStrategy {
	protected final T tp;

	public AbstractTpStrategy(T tp, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.tp = tp;
	}

	protected <B extends TypeBuilder> B getMutableType(ApiBuilder mutableApi) {
		var mutableType = mutableApi.allTypes.get(tp.getQualifiedName());
		if (mutableType == null) throw new ImpossibleChangeException();

		if (tp instanceof ClassDecl && mutableType instanceof ClassBuilder classBuilder) {
			return (B) classBuilder;
		} else if (tp instanceof InterfaceDecl && mutableType instanceof InterfaceBuilder interfaceBuilder) {
			return (B) interfaceBuilder;
		} else {
			throw new ImpossibleChangeException();
		}
	}

	protected List<TypeBuilder> getAllOtherMutableTypes(ApiBuilder mutableApi) {
		return mutableApi.allTypes.values().stream()
				.filter(t -> !t.qualifiedName.equals(tp.getQualifiedName()))
				.toList();
	}
}
