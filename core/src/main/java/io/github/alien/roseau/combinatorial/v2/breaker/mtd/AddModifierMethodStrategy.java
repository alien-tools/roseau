package io.github.alien.roseau.combinatorial.v2.breaker.mtd;

import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.Modifier;
import io.github.alien.roseau.api.utils.StringUtils;
import io.github.alien.roseau.combinatorial.builder.ApiBuilder;
import io.github.alien.roseau.combinatorial.builder.ClassBuilder;
import io.github.alien.roseau.combinatorial.builder.InterfaceBuilder;
import io.github.alien.roseau.combinatorial.builder.RecordBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.ImpossibleChangeException;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import static io.github.alien.roseau.api.model.Modifier.*;

public final class AddModifierMethodStrategy extends AbstractMtdStrategy {
	private final Modifier modifier;

	public AddModifierMethodStrategy(Modifier modifier, MethodDecl mtd, NewApiQueue queue) {
		super(mtd, queue, "AddModifier%sToMethod%sIn%s".formatted(
				modifier.toCapitalize(),
				StringUtils.splitSpecialCharsAndCapitalize(mtd.getErasure()),
				mtd.getContainingType().getPrettyQualifiedName())
		);

		this.modifier = modifier;
	}

	@Override
	protected void applyBreakToMutableApi(ApiBuilder mutableApi) throws ImpossibleChangeException {
		if (tpMbr.getModifiers().contains(modifier)) throw new ImpossibleChangeException();
		if (tpMbr.isPrivate() && modifier == ABSTRACT) throw new ImpossibleChangeException();
		if (tpMbr.isAbstract() && (modifier == DEFAULT || modifier == FINAL || modifier == STATIC || modifier == SYNCHRONIZED)) throw new ImpossibleChangeException();
		if (tpMbr.isDefault() && (modifier == ABSTRACT || modifier == STATIC)) throw new ImpossibleChangeException();
		if (tpMbr.isFinal() && modifier == ABSTRACT) throw new ImpossibleChangeException();
		if (tpMbr.isStatic() && (modifier == ABSTRACT || modifier == DEFAULT)) throw new ImpossibleChangeException();
		if (tpMbr.getModifiers().contains(SYNCHRONIZED) && modifier == ABSTRACT) throw new ImpossibleChangeException();

		var containingType = getContainingTypeFromMutableApi(mutableApi);
		if (containingType instanceof RecordBuilder) {
			if (modifier == ABSTRACT || modifier == DEFAULT) throw new ImpossibleChangeException();
		} else if (containingType instanceof ClassBuilder classBuilder) {
			if (modifier == DEFAULT) throw new ImpossibleChangeException();
			if (modifier == ABSTRACT && !classBuilder.modifiers.contains(ABSTRACT)) throw new ImpossibleChangeException();
		} else if (containingType instanceof InterfaceBuilder) {
			if (modifier == FINAL || modifier == SYNCHRONIZED) throw new ImpossibleChangeException();
		}

		var method = getMethodFrom(containingType);

		LOGGER.info("Adding modifier {} to method {}", modifier.toCapitalize(), tpMbr.getQualifiedName());

		method.modifiers.add(modifier);

		// TODO: For now we don't have hierarchy, so we don't need to update possible references
	}
}
