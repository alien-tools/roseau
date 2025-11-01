package io.github.alien.roseau.combinatorial.client;

import io.github.alien.roseau.api.model.*;
import io.github.alien.roseau.api.visit.AbstractApiVisitor;
import io.github.alien.roseau.api.visit.Visit;
import io.github.alien.roseau.combinatorial.writer.ClientWriter;

import java.util.Optional;

public final class ClientGeneratorVisitor extends AbstractApiVisitor {
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
				case EnumDecl e: generateTypeClients(e); break;
				case RecordDecl r: generateTypeClients(r); break;
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

	private void generateTypeClients(TypeDecl it) {
		if (!it.isNested() || it.isPublic()) {
			writer.writeTypeReference(it);
		}
	}

	private void generateClassClients(ClassDecl it) {
		generateTypeClients(it);

		if (!api.isEffectivelyFinal(it) && !it.isProtected()) {
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
		generateTypeClients(it);

		if (!api.isEffectivelyFinal(it) && !it.isProtected()) {
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

		if (!isTypeFinal(containingType)) {
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

		if (!isTypeFinal(containingType)) {
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

		var isContainingTypeFinal = isTypeFinal(containingType);
		if (!isContainingTypeFinal) {
			writer.writeMethodInheritanceInvocation(it, containingType);
		}

		if (containingType instanceof ClassDecl containingClass) {
			if (it.isPublic()) {
				if (!containingType.isAbstract()) {
					writer.writeMethodDirectInvocation(it, containingClass);
				} else if (!isContainingTypeFinal) {
					writer.writeMethodFullDirectInvocation(it, containingClass);
				}

				if (!api.isEffectivelyFinal(it) && !isContainingTypeFinal) {
					writer.writeMethodMinimalDirectInvocation(it, containingClass);
				}
			}

			if (!it.isAbstract() && !api.isEffectivelyFinal(it) && !isContainingTypeFinal) {
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

	private boolean isTypeFinal(TypeDecl type) {
		return type.isEnum() || type.isRecord() || api.isEffectivelyFinal(type);
	}
}
