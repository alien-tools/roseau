package io.github.alien.roseau.combinatorial.v2;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.model.reference.CachedTypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.combinatorial.builder.ParameterBuilder;
import io.github.alien.roseau.combinatorial.v2.breaker.cls.*;
import io.github.alien.roseau.combinatorial.v2.breaker.ctr.*;
import io.github.alien.roseau.combinatorial.v2.breaker.enmVal.RemoveEnumValueStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.fld.*;
import io.github.alien.roseau.combinatorial.v2.breaker.intf.RemoveInterfaceStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.mtd.*;
import io.github.alien.roseau.combinatorial.v2.breaker.rcdCpt.RemoveRecordComponentStrategy;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.*;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

public final class BreakingChangesGeneratorVisitor extends AbstractAPIVisitor {
	private final API api;

	private final NewApiQueue queue;

	private final TypeReferenceFactory typeReferenceFactory = new CachedTypeReferenceFactory();

	private final ITypeReference voidType = typeReferenceFactory.createPrimitiveTypeReference("void");
	private final ITypeReference intType = typeReferenceFactory.createPrimitiveTypeReference("int");
	private final ITypeReference booleanType = typeReferenceFactory.createTypeReference("java.lang.Boolean");
	private final ITypeReference threadType = typeReferenceFactory.createTypeReference("java.lang.Thread");
	private final ITypeReference charArrType = typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createPrimitiveTypeReference("char"), 1);

	private final ParameterBuilder intParam = new ParameterBuilder();
	private final ParameterBuilder booleanParam = new ParameterBuilder();
	private final ParameterBuilder threadParam = new ParameterBuilder();
	private final ParameterBuilder charArrParam = new ParameterBuilder();
	private final ParameterBuilder intVarargsParam = new ParameterBuilder();
	private final ParameterBuilder booleanVarargsParam = new ParameterBuilder();
	private final ParameterBuilder threadVarargsParam = new ParameterBuilder();
	private final ParameterBuilder charArrVarargsParam = new ParameterBuilder();

	public BreakingChangesGeneratorVisitor(API api, NewApiQueue queue) {
		this.api = api;
		this.queue = queue;

		intParam.name = "newParam";
		intParam.type = intType;
		booleanParam.name = "newParam";
		booleanParam.type = booleanType;
		threadParam.name = "newParam";
		threadParam.type = threadType;
		charArrParam.name = "newParam";
		charArrParam.type = charArrType;
		intVarargsParam.name = "newParam";
		intVarargsParam.type = intType;
		intVarargsParam.isVarargs = true;
		booleanVarargsParam.name = "newParam";
		booleanVarargsParam.type = booleanType;
		booleanVarargsParam.isVarargs = true;
		threadVarargsParam.name = "newParam";
		threadVarargsParam.type = threadType;
		threadVarargsParam.isVarargs = true;
		charArrVarargsParam.name = "newParam";
		charArrVarargsParam.type = charArrType;
		charArrVarargsParam.isVarargs = true;
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
		new RemoveTypeStrategy<>(e, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, e, queue).breakApi(api);
	}

	private void breakRecordDecl(RecordDecl r) {
		new RemoveTypeStrategy<>(r, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, r, queue).breakApi(api);

		new AddModifierTypeStrategy<>(Modifier.FINAL, r, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.FINAL, r, queue).breakApi(api);
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

		new AddParameterConstructorStrategy(intParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(booleanParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(threadParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(charArrParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(intVarargsParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(booleanVarargsParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(threadVarargsParam, c, queue).breakApi(api);
		new AddParameterConstructorStrategy(charArrVarargsParam, c, queue).breakApi(api);

		new AddExceptionConstructorStrategy(TypeReference.EXCEPTION, c, queue).breakApi(api);
		new RemoveExceptionConstructorStrategy(TypeReference.EXCEPTION, c, queue).breakApi(api);
	}

	private void breakEnumValueDecl(EnumValueDecl eV) {
		new RemoveEnumValueStrategy(eV, queue).breakApi(api);
	}

	private void breakRecordComponentDecl(RecordComponentDecl rC) {
		new RemoveRecordComponentStrategy(rC, queue).breakApi(api);
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

		new ChangeTypeFieldStrategy(intType, f, queue).breakApi(api);
		new ChangeTypeFieldStrategy(booleanType, f, queue).breakApi(api);
		new ChangeTypeFieldStrategy(threadType, f, queue).breakApi(api);
		new ChangeTypeFieldStrategy(charArrType, f, queue).breakApi(api);
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

		new ChangeTypeMethodStrategy(voidType, m, queue).breakApi(api);
		new ChangeTypeMethodStrategy(intType, m, queue).breakApi(api);
		new ChangeTypeMethodStrategy(booleanType, m, queue).breakApi(api);
		new ChangeTypeMethodStrategy(threadType, m, queue).breakApi(api);
		new ChangeTypeMethodStrategy(charArrType, m, queue).breakApi(api);

		new AddExceptionMethodStrategy(TypeReference.EXCEPTION, m, queue).breakApi(api);
		new RemoveExceptionMethodStrategy(TypeReference.EXCEPTION, m, queue).breakApi(api);
	}
}
