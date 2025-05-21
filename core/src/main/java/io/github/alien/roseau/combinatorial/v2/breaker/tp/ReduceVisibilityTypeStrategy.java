package io.github.alien.roseau.combinatorial.v2.breaker.tp;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ReduceVisibilityTypeStrategy<T extends TypeDecl> extends AbstractTpStrategy<T> {
	private final AccessModifier accessModifier;

	public ReduceVisibilityTypeStrategy(AccessModifier modifier, T tp, NewApiQueue queue) {
		super(tp, queue, "Reduce%sVisibilityTo%s".formatted(tp.getSimpleName(), modifier.toCapitalize()));

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		if (tp.getVisibility() == accessModifier) throw new ImpossibleChangeException();

		LOGGER.info("Reducing {} visibility to {}", tp.getQualifiedName(), accessModifier.toCapitalize());

		var mutableType = mutableApi.allTypes.get(tp.getQualifiedName());
		mutableType.visibility = accessModifier;
	}
}
