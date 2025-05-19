package io.github.alien.roseau.combinatorial.client;

import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.ConstructorDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.EnumValueDecl;
import io.github.alien.roseau.api.model.FieldDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.RecordComponentDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.visit.AbstractAPIVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.combinatorial.writer.ClientWriter;

import java.util.Optional;

public final class ClientGeneratorVisitor extends AbstractAPIVisitor {
	private final ClientWriter writer;
	private final API api;

	public ClientGeneratorVisitor(API api, ClientWriter writer) {
		this.api = api;
		this.writer = writer;
	}

	@Override
	public Visit symbol(Symbol it) {
		if (api.isExported(it)) {
			switch (it) {
				case EnumDecl e: generateClassClients(e); break;
				case RecordDecl r: generateClassClients(r); break;
				case ClassDecl c: generateClassClients(c); break;
				case InterfaceDecl i: generateInterfaceClients(i); break;
				case ConstructorDecl c: generateConstructorClients(c); break;
				case EnumValueDecl eV: generateEnumValueClients(eV); break;
				case FieldDecl f: generateFieldClients(f); break;
				case MethodDecl m: generateMethodClients(m); break;
				default: break;
			}
		} else {
			switch (it) {
				case RecordComponentDecl rC: generateRecordComponentClients(rC); break;
				default: break;
			}
		}

		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	private void generateClassClients(ClassDecl it) {
		writer.writeTypeReference(it);

		if (!api.isEffectivelyFinal(it) && !it.isSealed()) {
			writer.writeClassInheritance(it);
		}

		if (api.isCheckedException(it) || api.isUncheckedException(it)) {
			generateExceptionClients(it);
		}
	}

	private void generateExceptionClients(ClassDecl it) {
		writer.writeExceptionCatch(it);
		writer.writeExceptionThrows(it);

		if (!it.isEffectivelyAbstract()) {
			writer.writeExceptionThrow(it);
		}
	}

	private void generateInterfaceClients(InterfaceDecl it) {
		writer.writeTypeReference(it);

		if (!it.isSealed()) {
			writer.writeInterfaceExtension(it);
			writer.writeInterfaceImplementation(it);
		}
	}

	private void generateConstructorClients(ConstructorDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		if (!containingType.isClass()) return;
		var containingClass = (ClassDecl) containingType;

		if (it.isPublic() && !containingClass.isEffectivelyAbstract()) {
			writer.writeConstructorDirectInvocation(it, containingClass);
		}

		if (!api.isEffectivelyFinal(containingClass)) {
			writer.writeConstructorInheritanceInvocation(it, containingClass);
		}
	}

	private void generateEnumValueClients(EnumValueDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		if (containingType instanceof EnumDecl enumDecl) {
			writer.writeEnumValueRead(it, enumDecl);
		}
	}

	private void generateFieldClients(FieldDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		if (it.isPublic() && !containingType.isAbstract()) {
			writer.writeReadFieldThroughContainingType(it, containingType);

			if (!it.isFinal() && containingType.isClass()) {
				writer.writeWriteFieldThroughContainingType(it, containingType);
			}
		}

		if (!api.isEffectivelyFinal(containingType)) {
			writer.writeReadFieldThroughMethodCall(it, containingType);
			if (it.isPublic()) writer.writeReadFieldThroughSubType(it, containingType);

			if (!it.isFinal() && containingType.isClass()) {
				writer.writeWriteFieldThroughMethodCall(it, containingType);
				if (it.isPublic()) writer.writeWriteFieldThroughSubType(it, containingType);
			}
		}
	}

	private void generateMethodClients(MethodDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		if (!api.isEffectivelyFinal(containingType)) {
			writer.writeMethodInheritanceInvocation(it, containingType);
		}

		if (containingType instanceof ClassDecl containingClass) {
			if (it.isPublic() && !containingType.isAbstract()) {
				writer.writeMethodDirectInvocation(it, containingClass);
			}

			if (it.isPublic() && !api.isEffectivelyFinal(it) && !containingClass.isEnum() && !containingClass.isRecord()) {
				writer.writeMethodMinimalDirectInvocation(it, containingClass);
			}

			if (!it.isAbstract() && !api.isEffectivelyFinal(it)) {
				writer.writeMethodOverride(it, containingClass);
			}
		}

		if (containingType instanceof InterfaceDecl containingInterface) {
			if (it.isStatic()) {
				writer.writeMethodDirectInvocation(it, containingInterface);
			} else if (!api.isEffectivelyFinal(it)) {
				writer.writeMethodMinimalDirectInvocation(it, containingInterface);
			}

			if ((it.isDefault() || it.isStatic()) && !api.isEffectivelyFinal(it)) {
				writer.writeMethodOverride(it, containingInterface);
			}
		}
	}

	private void generateRecordComponentClients(RecordComponentDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		if (containingType instanceof RecordDecl recordDecl) {
			writer.writeRecordComponentRead(it, recordDecl);
		}
	}

	private Optional<TypeDecl> getContainingTypeFromTypeMember(TypeMemberDecl typeMemberDecl) {
		return api.resolver().resolve(typeMemberDecl.getContainingType());
	}
}
