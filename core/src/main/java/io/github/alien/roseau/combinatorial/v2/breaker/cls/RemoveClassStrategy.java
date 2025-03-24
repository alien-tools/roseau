package io.github.alien.roseau.combinatorial.v2.breaker.cls;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class RemoveClassStrategy extends AbstractClsStrategy {
	public RemoveClassStrategy(ClassDecl cls, NewApiQueue queue) {
		super(cls, queue, "RemoveClass" + cls.getSimpleName());
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Removing class " + cls.getPrettyQualifiedName());

		mutableApi.allTypes.remove(cls.getQualifiedName());
		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
