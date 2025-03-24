package io.github.alien.roseau.combinatorial.v2.breaker.tpDcl;

import io.github.alien.roseau.api.model.AccessModifier;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ReduceVisibilityTypeDeclStrategy extends AbstractTpDclStrategy {
	private final AccessModifier accessModifier;

	public ReduceVisibilityTypeDeclStrategy(AccessModifier modifier, TypeDecl tpDcl, NewApiQueue queue) {
		super(tpDcl, queue, "Reduce%sVisibilityTo%s".formatted(tpDcl.getSimpleName(), modifier));

		this.accessModifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) {
		LOGGER.info("Reducing {} visibility to {}", tpDcl.getQualifiedName(), accessModifier);

		mutableApi.allTypes.values().forEach(t -> {
			if (t.qualifiedName.equals(tpDcl.getQualifiedName())) {
				t.visibility = accessModifier;
			}
		});
		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
