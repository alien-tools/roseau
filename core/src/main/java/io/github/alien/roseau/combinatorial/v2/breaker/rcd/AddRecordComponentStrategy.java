package io.github.alien.roseau.combinatorial.v2.breaker.rcd;

import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordComponentBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class AddRecordComponentStrategy extends AbstractRcdStrategy {
	private final ITypeReference recordComponentType;
	private final boolean recordComponentIsVarargs;

	public AddRecordComponentStrategy(ITypeReference type, boolean isVarargs, RecordDecl rcd, NewApiQueue queue) {
		super(rcd, queue, "AddRecordComponent%s%sToRecord%s".formatted(
				type.getPrettyQualifiedName(),
				isVarargs ? "Varargs" : "",
				rcd.getPrettyQualifiedName())
		);

		this.recordComponentType = type;
		this.recordComponentIsVarargs = isVarargs;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		var recordComponents = tp.getRecordComponents();
		if (!recordComponents.isEmpty() && recordComponents.getLast().isVarargs()) throw new ImpossibleChangeException();

		var mutableRecord = getMutableBuilderFromMutableApi(mutableApi);
		var recordComponentBuilder = new RecordComponentBuilder();
		recordComponentBuilder.qualifiedName = "%s.cNew".formatted(tp.getQualifiedName());
		recordComponentBuilder.containingType = mutableApi.typeReferenceFactory.createTypeReference(tp.getQualifiedName());
		recordComponentBuilder.type = recordComponentType;
		recordComponentBuilder.isVarargs = recordComponentIsVarargs;
		mutableRecord.recordComponents.add(recordComponentBuilder);

		LOGGER.info("Adding record component {} to record {}", recordComponentType.getPrettyQualifiedName(), tp.getQualifiedName());
	}
}
