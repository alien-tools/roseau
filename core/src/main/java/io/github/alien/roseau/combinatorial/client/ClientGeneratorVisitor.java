package io.github.alien.roseau.combinatorial.client;

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

	public ClientGeneratorVisitor(ClientWriter writer) {
		this.writer = writer;
	}

	@Override
	public Visit symbol(Symbol it) {
		if (it.isExported()) {
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

		if (!it.isEffectivelyFinal() && !it.isSealed()) {
			writer.writeClassInheritance(it);
		}

		if (it.isCheckedException() || it.isUncheckedException()) {
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

		if (!containingClass.isEffectivelyFinal()) {
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

		if (!containingType.isEffectivelyFinal()) {
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

		if (!containingType.isEffectivelyFinal()) {
			writer.writeMethodInheritanceInvocation(it, containingType);
		}

		if (containingType instanceof ClassDecl containingClass) {
			if (it.isPublic() && !containingType.isAbstract()) {
				writer.writeMethodDirectInvocation(it, containingClass);
			}

			if (it.isPublic() && !it.isEffectivelyFinal() && !containingClass.isEnum() && !containingClass.isRecord()) {
				writer.writeMethodMinimalDirectInvocation(it, containingClass);
			}

			if (!it.isAbstract() && !it.isEffectivelyFinal()) {
				writer.writeMethodOverride(it, containingClass);
			}
		}

		if (containingType instanceof InterfaceDecl containingInterface) {
			if (it.isStatic()) {
				writer.writeMethodDirectInvocation(it, containingInterface);
			} else if (!it.isEffectivelyFinal()) {
				writer.writeMethodMinimalDirectInvocation(it, containingInterface);
			}

			if ((it.isDefault() || it.isStatic()) && !it.isEffectivelyFinal()) {
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

	private static Optional<TypeDecl> getContainingTypeFromTypeMember(TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.getContainingType().getResolvedApiType().isEmpty()) return Optional.empty();

		return Optional.of(typeMemberDecl.getContainingType().getResolvedApiType().get());
	}
}
