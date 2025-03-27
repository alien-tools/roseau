package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ReduceVisibilityTypeStrategy extends AbstractTpStrategy {
	private final AccessModifier accessModifier;

	public ReduceVisibilityTypeStrategy(AccessModifier modifier, TypeDecl tp, NewApiQueue queue) {
		super(tp, queue, "Reduce%sVisibilityTo%s".formatted(tp.getSimpleName(), modifier.toCapitalize()));

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		LOGGER.info("Reducing {} visibility to {}", tp.getQualifiedName(), accessModifier.toCapitalize());

		var mutableType = mutableApi.allTypes.get(tp.getQualifiedName());
		if (mutableType == null) throw new ImpossibleChangeException();

		mutableType.visibility = accessModifier;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
