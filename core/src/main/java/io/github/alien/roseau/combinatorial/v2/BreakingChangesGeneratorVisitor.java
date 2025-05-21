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

	private final TypeReferenceFactory typeReferenceFactory = new CachedTypeReferenceFactory();

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
		new RemoveTypeStrategy<>(t, queue).breakApi(api);

		new ReduceVisibilityTypeStrategy<>(AccessModifier.PACKAGE_PRIVATE, t, queue).breakApi(api);

		new AddImplementedInterfaceTypeStrategy<>(t, queue).breakApi(api);
		for (var interfaceTypeRef : t.getImplementedInterfaces()) {
			var interfaceDecl = interfaceTypeRef.getResolvedApiType().orElse(null);
			if (interfaceDecl == null) continue;

			new RemoveImplementedInterfaceTypeStrategy<>(interfaceDecl, t, queue).breakApi(api);
		}
	}

	private void breakRecordDecl(RecordDecl r) {
		new AddModifierTypeStrategy<>(Modifier.FINAL, r, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.FINAL, r, queue).breakApi(api);

		for (var paramType: types) {
			new AddRecordComponentStrategy(paramType, false, r, queue).breakApi(api);
			new AddRecordComponentStrategy(paramType, true, r, queue).breakApi(api);
		}

		for (var recordComponentIndex = 0; recordComponentIndex < r.getRecordComponents().size(); recordComponentIndex++) {
			for (var type : types) {
				new ChangeRecordComponentStrategy(recordComponentIndex, type, false, r, queue).breakApi(api);
				new ChangeRecordComponentStrategy(recordComponentIndex, type, true, r, queue).breakApi(api);
			}

			new RemoveRecordComponentStrategy(recordComponentIndex, r, queue).breakApi(api);
		}
	}

	private void breakClassDecl(ClassDecl c) {
		new AddModifierTypeStrategy<>(Modifier.ABSTRACT, c, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.ABSTRACT, c, queue).breakApi(api);
		new AddModifierTypeStrategy<>(Modifier.FINAL, c, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.FINAL, c, queue).breakApi(api);
		new AddModifierTypeStrategy<>(Modifier.SEALED, c, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.SEALED, c, queue).breakApi(api);
		new AddModifierTypeStrategy<>(Modifier.NON_SEALED, c, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.NON_SEALED, c, queue).breakApi(api);

		new AddAbstractMethodTypeStrategy<>(c, queue).breakApi(api);

		new AddSuperClassClassStrategy(c, queue).breakApi(api);
		new RemoveSuperClassClassStrategy(c, queue).breakApi(api);
	}

	private void breakInterfaceDecl(InterfaceDecl i) {
		new AddModifierTypeStrategy<>(Modifier.ABSTRACT, i, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.ABSTRACT, i, queue).breakApi(api);
		new AddModifierTypeStrategy<>(Modifier.SEALED, i, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.SEALED, i, queue).breakApi(api);
		new AddModifierTypeStrategy<>(Modifier.NON_SEALED, i, queue).breakApi(api);
		new RemoveModifierTypeStrategy<>(Modifier.NON_SEALED, i, queue).breakApi(api);

		new AddAbstractMethodTypeStrategy<>(i, queue).breakApi(api);
	}

	private void breakConstructorDecl(ConstructorDecl c) {
		new RemoveConstructorStrategy(c, queue).breakApi(api);

		new ChangeVisibilityConstructorStrategy(AccessModifier.PUBLIC, c, queue).breakApi(api);
		new ChangeVisibilityConstructorStrategy(AccessModifier.PROTECTED, c, queue).breakApi(api);
		new ChangeVisibilityConstructorStrategy(AccessModifier.PACKAGE_PRIVATE, c, queue).breakApi(api);
		new ChangeVisibilityConstructorStrategy(AccessModifier.PRIVATE, c, queue).breakApi(api);

		for (var type : types) {
			new AddParameterConstructorStrategy(type, false, c, queue).breakApi(api);
			new AddParameterConstructorStrategy(type, true, c, queue).breakApi(api);
		}

		for (var paramIndex = 0; paramIndex < c.getParameters().size(); paramIndex++) {
			for (var type : types) {
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

		for (var paramType: types) {
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

		for (var type : returnTypes) {
			new ChangeTypeMethodStrategy(type, m, queue).breakApi(api);
		}

		for (var type : types) {
			new AddParameterMethodStrategy(type, false, m, queue).breakApi(api);
			new AddParameterMethodStrategy(type, true, m, queue).breakApi(api);
		}

		for (var paramIndex = 0; paramIndex < m.getParameters().size(); paramIndex++) {
			for (var type : types) {
				new ChangeParameterMethodStrategy(paramIndex, type, false, m, queue).breakApi(api);
				new ChangeParameterMethodStrategy(paramIndex, type, true, m, queue).breakApi(api);
			}

			new RemoveParameterMethodStrategy(paramIndex, m, queue).breakApi(api);
		}

		new AddExceptionMethodStrategy(TypeReference.IO_EXCEPTION, m, queue).breakApi(api);
		new RemoveExceptionMethodStrategy(TypeReference.IO_EXCEPTION, m, queue).breakApi(api);
	}
}
