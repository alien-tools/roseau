package io.github.alien.roseau.combinatorial.v2;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.model.reference.CachedTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.combinatorial.v2.breaker.cls.*;
import io.github.alien.roseau.combinatorial.v2.breaker.ctr.*;
import io.github.alien.roseau.combinatorial.v2.breaker.enmVal.RemoveEnumValueStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.fld.*;
import io.github.alien.roseau.combinatorial.v2.breaker.intf.RemoveInterfaceStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.mtd.*;
import io.github.alien.roseau.combinatorial.v2.breaker.rcd.AddRecordComponentStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.rcd.ChangeRecordComponentStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.rcd.RemoveRecordComponentStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.*;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.List;

public final class BreakingChangesGeneratorVisitor extends AbstractAPIVisitor {
	private final API api;

	private final NewApiQueue queue;

	private final TypeReferenceFactory typeReferenceFactory = new CachedTypeReferenceFactory();

	private final ITypeReference voidType = typeReferenceFactory.createPrimitiveTypeReference("void");
	private final ITypeReference intType = typeReferenceFactory.createPrimitiveTypeReference("int");
	private final ITypeReference booleanType = typeReferenceFactory.createTypeReference("java.lang.Boolean");
	private final ITypeReference threadType = typeReferenceFactory.createTypeReference("java.lang.Thread");
	private final ITypeReference charArrType = typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createPrimitiveTypeReference("char"), 1);

	private final List<ITypeReference> paramTypes = List.of(intType, booleanType, threadType, charArrType);
	private final List<ITypeReference> types = List.of(voidType, intType, booleanType, threadType, charArrType);

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
			case FieldDecl f: breakFieldDecl(f); break;
			case MethodDecl m: breakMethodDecl(m); break;
			default: break;
		}

		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	private void breakEnumDecl(EnumDecl e) {
		new RemoveTypeStrategy<>(e, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, e, queue).breakApi(api);
	}

	private void breakRecordDecl(RecordDecl r) {
		new RemoveTypeStrategy<>(r, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, r, queue).breakApi(api);

		new AddModifierTypeStrategy<>(Modifier.FINAL, r, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.FINAL, r, queue).breakApi(api);

		for (var paramType: paramTypes) {
			new AddRecordComponentStrategy(paramType, false, r, queue).breakApi(api);
			new AddRecordComponentStrategy(paramType, true, r, queue).breakApi(api);
		}

		for (var recordComponentIndex = 0; recordComponentIndex < r.getRecordComponents().size(); recordComponentIndex++) {
			for (var type : paramTypes) {
				new ChangeRecordComponentStrategy(recordComponentIndex, type, false, r, queue).breakApi(api);
				new ChangeRecordComponentStrategy(recordComponentIndex, type, true, r, queue).breakApi(api);
			}

			new RemoveRecordComponentStrategy(recordComponentIndex, r, queue).breakApi(api);
		}
	}

	private void breakClassDecl(ClassDecl c) {
		new RemoveTypeStrategy<>(c, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, c, queue).breakApi(api);

		new AddModifierClassStrategy(Modifier.ABSTRACT, c, queue).breakApi(api);
		new RemoveModifierClassStrategy(Modifier.ABSTRACT, c, queue).breakApi(api);
		new AddModifierClassStrategy(Modifier.FINAL, c, queue).breakApi(api);
		new RemoveModifierClassStrategy(Modifier.FINAL, c, queue).breakApi(api);

		new AddMethodAbstractClassStrategy(c, queue).breakApi(api);
	}

	private void breakInterfaceDecl(InterfaceDecl i) {
		new RemoveInterfaceStrategy(i, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, i, queue).breakApi(api);

		new AddModifierTypeStrategy<>(Modifier.ABSTRACT, i, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.ABSTRACT, i, queue).breakApi(api);

		new AddMethodTypeStrategy<>(i, queue).breakApi(api);
	}

	private void breakConstructorDecl(ConstructorDecl c) {
		new RemoveConstructorStrategy(c, queue).breakApi(api);

		new ChangeVisibilityConstructorStrategy(AccessModifier.PUBLIC, c, queue).breakApi(api);
		new ChangeVisibilityConstructorStrategy(AccessModifier.PROTECTED, c, queue).breakApi(api);
		new ChangeVisibilityConstructorStrategy(AccessModifier.PACKAGE_PRIVATE, c, queue).breakApi(api);
		new ChangeVisibilityConstructorStrategy(AccessModifier.PRIVATE, c, queue).breakApi(api);

		for (var type : paramTypes) {
			new AddParameterConstructorStrategy(type, false, c, queue).breakApi(api);
			new AddParameterConstructorStrategy(type, true, c, queue).breakApi(api);
		}

		for (var paramIndex = 0; paramIndex < c.getParameters().size(); paramIndex++) {
			for (var type : paramTypes) {
				new ChangeParameterConstructorStrategy(paramIndex, type, false, c, queue).breakApi(api);
				new ChangeParameterConstructorStrategy(paramIndex, type, true, c, queue).breakApi(api);
			}

			new RemoveParameterConstructorStrategy(paramIndex, c, queue).breakApi(api);
		}

		new AddExceptionConstructorStrategy(TypeReference.IO_EXCEPTION, c, queue).breakApi(api);
		new RemoveExceptionConstructorStrategy(TypeReference.IO_EXCEPTION, c, queue).breakApi(api);
	}

	private void breakEnumValueDecl(EnumValueDecl eV) {
		new RemoveEnumValueStrategy(eV, queue).breakApi(api);
	}

	private void breakFieldDecl(FieldDecl f) {
		new RemoveFieldStrategy(f, queue).breakApi(api);

		new ChangeVisibilityFieldStrategy(AccessModifier.PUBLIC, f, queue).breakApi(api);
		new ChangeVisibilityFieldStrategy(AccessModifier.PROTECTED, f, queue).breakApi(api);
		new ChangeVisibilityFieldStrategy(AccessModifier.PACKAGE_PRIVATE, f, queue).breakApi(api);
		new ChangeVisibilityFieldStrategy(AccessModifier.PRIVATE, f, queue).breakApi(api);

		new AddModifierFieldStrategy(Modifier.FINAL, f, queue).breakApi(api);
		new RemoveModifierFieldStrategy(Modifier.FINAL, f, queue).breakApi(api);
		new AddModifierFieldStrategy(Modifier.STATIC, f, queue).breakApi(api);
		new RemoveModifierFieldStrategy(Modifier.STATIC, f, queue).breakApi(api);

		for (var paramType: paramTypes) {
			new ChangeTypeFieldStrategy(paramType, f, queue).breakApi(api);
		}
	}

	private void breakMethodDecl(MethodDecl m) {
		new RemoveMethodStrategy(m, queue).breakApi(api);

		new ChangeVisibilityMethodStrategy(AccessModifier.PUBLIC, m, queue).breakApi(api);
		new ChangeVisibilityMethodStrategy(AccessModifier.PROTECTED, m, queue).breakApi(api);
		new ChangeVisibilityMethodStrategy(AccessModifier.PACKAGE_PRIVATE, m, queue).breakApi(api);
		new ChangeVisibilityMethodStrategy(AccessModifier.PRIVATE, m, queue).breakApi(api);

		new AddModifierMethodStrategy(Modifier.ABSTRACT, m, queue).breakApi(api);
		new RemoveModifierMethodStrategy(Modifier.ABSTRACT, m, queue).breakApi(api);
		new AddModifierMethodStrategy(Modifier.DEFAULT, m, queue).breakApi(api);
		new RemoveModifierMethodStrategy(Modifier.DEFAULT, m, queue).breakApi(api);
		new AddModifierMethodStrategy(Modifier.FINAL, m, queue).breakApi(api);
		new RemoveModifierMethodStrategy(Modifier.FINAL, m, queue).breakApi(api);
		new AddModifierMethodStrategy(Modifier.STATIC, m, queue).breakApi(api);
		new RemoveModifierMethodStrategy(Modifier.STATIC, m, queue).breakApi(api);
		new AddModifierMethodStrategy(Modifier.SYNCHRONIZED, m, queue).breakApi(api);
		new RemoveModifierMethodStrategy(Modifier.SYNCHRONIZED, m, queue).breakApi(api);

		for (var type : types) {
			new ChangeTypeMethodStrategy(type, m, queue).breakApi(api);
		}

		for (var type : paramTypes) {
			new AddParameterMethodStrategy(type, false, m, queue).breakApi(api);
			new AddParameterMethodStrategy(type, true, m, queue).breakApi(api);
		}

		for (var paramIndex = 0; paramIndex < m.getParameters().size(); paramIndex++) {
			for (var type : paramTypes) {
				new ChangeParameterMethodStrategy(paramIndex, type, false, m, queue).breakApi(api);
				new ChangeParameterMethodStrategy(paramIndex, type, true, m, queue).breakApi(api);
			}

			new RemoveParameterMethodStrategy(paramIndex, m, queue).breakApi(api);
		}

		new AddExceptionMethodStrategy(TypeReference.IO_EXCEPTION, m, queue).breakApi(api);
		new RemoveExceptionMethodStrategy(TypeReference.IO_EXCEPTION, m, queue).breakApi(api);
	}
}
