package io.github.alien.roseau.combinatorial.v2;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.model.reference.*;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.combinatorial.v2.breaker.cls.*;
import io.github.alien.roseau.combinatorial.v2.breaker.ctr.*;
import io.github.alien.roseau.combinatorial.v2.breaker.enmVal.*;
import io.github.alien.roseau.combinatorial.v2.breaker.fld.*;
import io.github.alien.roseau.combinatorial.v2.breaker.mtd.*;
import io.github.alien.roseau.combinatorial.v2.breaker.rcd.*;
import io.github.alien.roseau.combinatorial.v2.breaker.tp.*;
import io.github.alien.roseau.combinatorial.v2.queue.NewApiQueue;

import java.util.List;
import java.util.stream.Stream;

public final class BreakingChangesGeneratorVisitor extends AbstractAPIVisitor {
	private final API api;

	private final NewApiQueue queue;

	private final TypeReferenceFactory typeReferenceFactory = new CachingTypeReferenceFactory();

	private final List<ITypeReference> types = List.of(
			typeReferenceFactory.createPrimitiveTypeReference("long"),
			typeReferenceFactory.createPrimitiveTypeReference("byte"),
			typeReferenceFactory.createTypeReference("java.lang.Integer"),
			typeReferenceFactory.createTypeReference("java.security.SecureRandom"),
			typeReferenceFactory.createTypeReference("java.util.Date"),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createPrimitiveTypeReference("long"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createPrimitiveTypeReference("byte"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createTypeReference("java.lang.Integer"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createTypeReference("java.security.SecureRandom"), 1),
			typeReferenceFactory.createArrayTypeReference(typeReferenceFactory.createTypeReference("java.util.Date"), 1)
	);
	private final List<ITypeReference> returnTypes = Stream
			.concat(types.stream(), Stream.of(new PrimitiveTypeReference("void")))
			.toList();

	public BreakingChangesGeneratorVisitor(API api, NewApiQueue queue) {
		this.api = api;
		this.queue = queue;
	}

	public Visit symbol(Symbol it) {
		switch (it) {
			case EnumDecl e:
				breakTypeDecl(e);
				break;
			case RecordDecl r:
				breakTypeDecl(r);
				breakRecordDecl(r);
				break;
			case ClassDecl c:
				breakTypeDecl(c);
				breakClassDecl(c);
				break;
			case InterfaceDecl i:
				breakTypeDecl(i);
				breakInterfaceDecl(i);
				break;
			case ConstructorDecl c:
				breakConstructorDecl(c);
				break;
			case EnumValueDecl eV:
				breakEnumValueDecl(eV);
				break;
			case FieldDecl f:
				breakFieldDecl(f);
				break;
			case MethodDecl m:
				breakMethodDecl(m);
				break;
			default:
				break;
		}

		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	private void breakTypeDecl(TypeDecl t) {
		new RemoveTypeStrategy<>(t, queue, api).breakApi();

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, t, queue, api).breakApi();

		new AddImplementedInterfaceTypeStrategy<>(t, queue, api).breakApi();
		for (var interfaceTypeRef : t.getImplementedInterfaces()) {
			new RemoveImplementedInterfaceTypeStrategy<>(interfaceTypeRef.getQualifiedName(), t, queue, api).breakApi();
		}
	}

	private void breakRecordDecl(RecordDecl r) {
		new AddModifierTypeStrategy<>(Modifier.FINAL, r, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.FINAL, r, queue, api).breakApi();

		for (var paramType: types) {
			new AddRecordComponentStrategy(paramType, false, r, queue, api).breakApi();
			new AddRecordComponentStrategy(paramType, true, r, queue, api).breakApi();
		}

		for (var recordComponentIndex = 0; recordComponentIndex < r.getRecordComponents().size(); recordComponentIndex++) {
			for (var type : types) {
				new ChangeRecordComponentStrategy(recordComponentIndex, type, false, r, queue, api).breakApi();
				new ChangeRecordComponentStrategy(recordComponentIndex, type, true, r, queue, api).breakApi();
			}

			new RemoveRecordComponentStrategy(recordComponentIndex, r, queue, api).breakApi();
		}
	}

	private void breakClassDecl(ClassDecl c) {
		new AddModifierTypeStrategy<>(Modifier.ABSTRACT, c, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.ABSTRACT, c, queue, api).breakApi();
		new AddModifierTypeStrategy<>(Modifier.FINAL, c, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.FINAL, c, queue, api).breakApi();
		new AddModifierTypeStrategy<>(Modifier.SEALED, c, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.SEALED, c, queue, api).breakApi();
		new AddModifierTypeStrategy<>(Modifier.NON_SEALED, c, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.NON_SEALED, c, queue, api).breakApi();

		new AddAbstractMethodTypeStrategy<>(c, queue, api).breakApi();

		new AddSuperClassClassStrategy(c, queue, api).breakApi();
		new RemoveSuperClassClassStrategy(c, queue, api).breakApi();
	}

	private void breakInterfaceDecl(InterfaceDecl i) {
		new AddModifierTypeStrategy<>(Modifier.ABSTRACT, i, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.ABSTRACT, i, queue, api).breakApi();
		new AddModifierTypeStrategy<>(Modifier.SEALED, i, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.SEALED, i, queue, api).breakApi();
		new AddModifierTypeStrategy<>(Modifier.NON_SEALED, i, queue, api).breakApi();
		new RemoveModifierTypeStrategy<>(Modifier.NON_SEALED, i, queue, api).breakApi();

		new AddAbstractMethodTypeStrategy<>(i, queue, api).breakApi();
	}

	private void breakConstructorDecl(ConstructorDecl c) {
		new RemoveConstructorStrategy(c, queue, api).breakApi();

		new ChangeVisibilityConstructorStrategy(AccessModifier.PUBLIC, c, queue, api).breakApi();
		new ChangeVisibilityConstructorStrategy(AccessModifier.PROTECTED, c, queue, api).breakApi();
		new ChangeVisibilityConstructorStrategy(AccessModifier.PACKAGE_PRIVATE, c, queue, api).breakApi();
		new ChangeVisibilityConstructorStrategy(AccessModifier.PRIVATE, c, queue, api).breakApi();

		for (var type : types) {
			new AddParameterConstructorStrategy(type, false, c, queue, api).breakApi();
			new AddParameterConstructorStrategy(type, true, c, queue, api).breakApi();
		}

		for (var paramIndex = 0; paramIndex < c.getParameters().size(); paramIndex++) {
			for (var type : types) {
				new ChangeParameterConstructorStrategy(paramIndex, type, false, c, queue, api).breakApi();
				new ChangeParameterConstructorStrategy(paramIndex, type, true, c, queue, api).breakApi();
			}

			new RemoveParameterConstructorStrategy(paramIndex, c, queue, api).breakApi();
		}

		new AddExceptionConstructorStrategy(TypeReference.IO_EXCEPTION, c, queue, api).breakApi();
		new RemoveExceptionConstructorStrategy(TypeReference.IO_EXCEPTION, c, queue, api).breakApi();
	}

	private void breakEnumValueDecl(EnumValueDecl eV) {
		new RemoveEnumValueStrategy(eV, queue, api).breakApi();
	}

	private void breakFieldDecl(FieldDecl f) {
		new RemoveFieldStrategy(f, queue, api).breakApi();

		new ChangeVisibilityFieldStrategy(AccessModifier.PUBLIC, f, queue, api).breakApi();
		new ChangeVisibilityFieldStrategy(AccessModifier.PROTECTED, f, queue, api).breakApi();
		new ChangeVisibilityFieldStrategy(AccessModifier.PACKAGE_PRIVATE, f, queue, api).breakApi();
		new ChangeVisibilityFieldStrategy(AccessModifier.PRIVATE, f, queue, api).breakApi();

		new AddModifierFieldStrategy(Modifier.FINAL, f, queue, api).breakApi();
		new RemoveModifierFieldStrategy(Modifier.FINAL, f, queue, api).breakApi();
		new AddModifierFieldStrategy(Modifier.STATIC, f, queue, api).breakApi();
		new RemoveModifierFieldStrategy(Modifier.STATIC, f, queue, api).breakApi();

		for (var paramType: types) {
			new ChangeTypeFieldStrategy(paramType, f, queue, api).breakApi();
		}
	}

	private void breakMethodDecl(MethodDecl m) {
		new RemoveMethodStrategy(m, queue, api).breakApi();

		new ChangeVisibilityMethodStrategy(AccessModifier.PUBLIC, m, queue, api).breakApi();
		new ChangeVisibilityMethodStrategy(AccessModifier.PROTECTED, m, queue, api).breakApi();
		new ChangeVisibilityMethodStrategy(AccessModifier.PACKAGE_PRIVATE, m, queue, api).breakApi();
		new ChangeVisibilityMethodStrategy(AccessModifier.PRIVATE, m, queue, api).breakApi();

		new AddModifierMethodStrategy(Modifier.ABSTRACT, m, queue, api).breakApi();
		new RemoveModifierMethodStrategy(Modifier.ABSTRACT, m, queue, api).breakApi();
		new AddModifierMethodStrategy(Modifier.DEFAULT, m, queue, api).breakApi();
		new RemoveModifierMethodStrategy(Modifier.DEFAULT, m, queue, api).breakApi();
		new AddModifierMethodStrategy(Modifier.FINAL, m, queue, api).breakApi();
		new RemoveModifierMethodStrategy(Modifier.FINAL, m, queue, api).breakApi();
		new AddModifierMethodStrategy(Modifier.STATIC, m, queue, api).breakApi();
		new RemoveModifierMethodStrategy(Modifier.STATIC, m, queue, api).breakApi();
		new AddModifierMethodStrategy(Modifier.SYNCHRONIZED, m, queue, api).breakApi();
		new RemoveModifierMethodStrategy(Modifier.SYNCHRONIZED, m, queue, api).breakApi();

		for (var type : returnTypes) {
			new ChangeTypeMethodStrategy(type, m, queue, api).breakApi();
		}

		for (var type : types) {
			new AddParameterMethodStrategy(type, false, m, queue, api).breakApi();
			new AddParameterMethodStrategy(type, true, m, queue, api).breakApi();
		}

		for (var paramIndex = 0; paramIndex < m.getParameters().size(); paramIndex++) {
			for (var type : types) {
				new ChangeParameterMethodStrategy(paramIndex, type, false, m, queue, api).breakApi();
				new ChangeParameterMethodStrategy(paramIndex, type, true, m, queue, api).breakApi();
			}

			new RemoveParameterMethodStrategy(paramIndex, m, queue, api).breakApi();
		}

		new AddExceptionMethodStrategy(TypeReference.IO_EXCEPTION, m, queue, api).breakApi();
		new RemoveExceptionMethodStrategy(TypeReference.IO_EXCEPTION, m, queue, api).breakApi();
	}
}
