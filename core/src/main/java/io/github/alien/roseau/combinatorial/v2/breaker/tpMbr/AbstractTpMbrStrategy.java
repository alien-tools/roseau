package io.github.alien.roseau.combinatorial.v2.breaker.tpMbr;

import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.combinatorial.builder.*;
import io.github.alien.roseau.combinatorial.v2.breaker.AbstractApiBreakerStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public abstract class AbstractTpMbrStrategy<T extends TypeMemberDecl> extends AbstractApiBreakerStrategy {
	protected final T tpMbr;

	public AbstractTpMbrStrategy(T tpMbr, NewApiQueue queue, String strategyName) {
		super(queue, strategyName);

		this.tpMbr = tpMbr;
	}

	protected TypeBuilder getContainingTypeFromMutableApi(ApiBuilder mutableApi) {
		var containingType = mutableApi.allTypes.get(tpMbr.getContainingType().getQualifiedName());
		if (containingType == null) throw new RuntimeException();

		return containingType;
	}

	protected ClassBuilder getContainingClassFromMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof ClassBuilder classBuilder) return classBuilder;

		throw new RuntimeException();
	}

	protected EnumBuilder getContainingEnumFromMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof EnumBuilder enumBuilder) return enumBuilder;

		throw new RuntimeException();
	}

	protected RecordBuilder getContainingRecordFromMutableApi(ApiBuilder mutableApi) {
		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof RecordBuilder recordBuilder) return recordBuilder;

		throw new RuntimeException();
	}
}
