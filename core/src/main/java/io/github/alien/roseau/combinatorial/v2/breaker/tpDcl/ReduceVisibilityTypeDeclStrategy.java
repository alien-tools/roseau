package io.github.alien.roseau.combinatorial.v2.breaker.tpDcl;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ReduceVisibilityTypeDeclStrategy extends AbstractTpDclStrategy {
	private final AccessModifier accessModifier;

	public ReduceVisibilityTypeDeclStrategy(AccessModifier modifier, TypeDecl tpDcl, NewApiQueue queue) {
		super(tpDcl, queue, "Reduce%sVisibilityTo%s".formatted(tpDcl.getSimpleName(), modifier.toCapitalize()));

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		LOGGER.info("Reducing {} visibility to {}", tpDcl.getQualifiedName(), accessModifier.toCapitalize());

		var mutableType = mutableApi.allTypes.get(tpDcl.getQualifiedName());
		if (mutableType == null) throw new ImpossibleChangeException();

		mutableType.visibility = accessModifier;

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
