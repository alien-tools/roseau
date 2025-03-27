package io.github.alien.roseau.combinatorial.v2;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.combinatorial.v2.breaker.cls.AddMethodAbstractClassStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.cls.AddModifierClassStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.cls.RemoveModifierClassStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.ctr.RemoveConstructorStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.enmVal.RemoveEnumValueStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.fld.RemoveFieldStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.intf.RemoveInterfaceStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.mtd.RemoveMethodStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.rcdCpt.RemoveRecordComponentStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddMethodTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.AddModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.ReduceVisibilityTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.RemoveModifierTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.RemoveTypeStrategy;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class BreakingChangesGeneratorVisitor extends AbstractAPIVisitor {
	private final API api;

	private final NewApiQueue queue;

	public BreakingChangesGeneratorVisitor(API api, NewApiQueue queue) {
		this.api = api;
		this.queue = queue;
	}

	public Visit symbol(Symbol it) {
		switch (it) {
			case EnumDecl e: breakEnumDecl(e); break;
			case RecordDecl r: breakRecordDecl(r); break;
			case ClassDecl c: breakClassDecl(c); break;
			case InterfaceDecl i: breakInterfaceDecl(i); break;
			case ConstructorDecl c: breakConstructorDecl(c); break;
			case EnumValueDecl eV: breakEnumValueDecl(eV); break;
			case RecordComponentDecl rC: breakRecordComponentDecl(rC); break;
			case FieldDecl f: breakFieldDecl(f); break;
			case MethodDecl m: breakMethodDecl(m); break;
			default: break;
		}

		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	private void breakEnumDecl(EnumDecl e) {
		new RemoveTypeStrategy(e, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy(AccessModifier.PACKAGE_PRIVATE, e, queue).breakApi(api);
	}

	private void breakRecordDecl(RecordDecl r) {
		new RemoveTypeStrategy(r, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy(AccessModifier.PACKAGE_PRIVATE, r, queue).breakApi(api);

		new AddModifierTypeStrategy(Modifier.FINAL, r, queue).breakApi(api);
		new RemoveModifierTypeStrategy(Modifier.FINAL, r, queue).breakApi(api);
	}

	private void breakClassDecl(ClassDecl c) {
		new RemoveTypeStrategy(c, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy(AccessModifier.PACKAGE_PRIVATE, c, queue).breakApi(api);

		new AddModifierClassStrategy(Modifier.ABSTRACT, c, queue).breakApi(api);
		new AddModifierClassStrategy(Modifier.FINAL, c, queue).breakApi(api);
		new RemoveModifierClassStrategy(Modifier.ABSTRACT, c, queue).breakApi(api);
		new RemoveModifierClassStrategy(Modifier.FINAL, c, queue).breakApi(api);

		new AddMethodAbstractClassStrategy(c, queue).breakApi(api);
	}

	private void breakInterfaceDecl(InterfaceDecl i) {
		new RemoveInterfaceStrategy(i, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy(AccessModifier.PACKAGE_PRIVATE, i, queue).breakApi(api);

		new AddModifierTypeStrategy(Modifier.ABSTRACT, i, queue).breakApi(api);
		new RemoveModifierTypeStrategy(Modifier.ABSTRACT, i, queue).breakApi(api);

		new AddMethodTypeStrategy(i, queue).breakApi(api);
	}

	private void breakConstructorDecl(ConstructorDecl c) {
		new RemoveConstructorStrategy(c, queue).breakApi(api);
	}

	private void breakEnumValueDecl(EnumValueDecl eV) {
		new RemoveEnumValueStrategy(eV, queue).breakApi(api);
	}

	private void breakRecordComponentDecl(RecordComponentDecl rC) {
		new RemoveRecordComponentStrategy(rC, queue).breakApi(api);
	}

	private void breakFieldDecl(FieldDecl f) {
		new RemoveFieldStrategy(f, queue).breakApi(api);
	}

	private void breakMethodDecl(MethodDecl m) {
		new RemoveMethodStrategy(m, queue).breakApi(api);
	}
}
