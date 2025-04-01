package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class ChangeRecordComponentStrategy extends AbstractRcdStrategy {
	private final int recordComponentIndex;
	private final ITypeReference recordComponentType;
	private final boolean recordComponentIsVarargs;

	public ChangeRecordComponentStrategy(int index, ITypeReference type, boolean isVarargs, RecordDecl rcd, NewApiQueue queue) {
		super(rcd, queue, "ChangeRecordComponent%dTo%s%sFromRecord%s".formatted(
				index,
				type.getPrettyQualifiedName(),
				isVarargs ? "Varargs" : "",
				rcd.getPrettyQualifiedName())
		);

		this.recordComponentIndex = index;
		this.recordComponentType = type;
		this.recordComponentIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var recordComponents = tp.getRecordComponents();
		if (recordComponentIndex < 0 || recordComponentIndex >= recordComponents.size()) throw new ImpossibleChangeException();
		if (recordComponentIsVarargs && recordComponentIndex != recordComponents.size() - 1) throw new ImpossibleChangeException();

		var mutableRecord = getMutableBuilderFromMutableApi(mutableApi);
		var currentRecordComponent = mutableRecord.recordComponents.get(recordComponentIndex);
		if (currentRecordComponent == null) throw new ImpossibleChangeException();
		if (currentRecordComponent.type.equals(recordComponentType) && currentRecordComponent.isVarargs == recordComponentIsVarargs) throw new ImpossibleChangeException();

		LOGGER.info("Changing record component at index {} from record {}", recordComponentIndex, tp.getQualifiedName());

		currentRecordComponent.type = recordComponentType;
		currentRecordComponent.isVarargs = recordComponentIsVarargs;
	}
}
