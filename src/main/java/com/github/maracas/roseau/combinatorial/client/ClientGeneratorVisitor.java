package com.github.maracas.roseau.combinatorial.client;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;

import java.util.Optional;

public class ClientGeneratorVisitor extends AbstractAPIVisitor {
	private final ClientWriter writer;

	public ClientGeneratorVisitor(ClientWriter writer) {
		this.writer = writer;
	}

	public Visit symbol(Symbol it) {
		if (it.isExported()) {
			switch (it) {
				case EnumDecl e: generateClassClients(e); break;
				case RecordDecl r: generateClassClients(r); break;
				case ClassDecl c: generateClassClients(c); break;
				case InterfaceDecl i: generateInterfaceClients(i); break;
				case ConstructorDecl c: generateConstructorClients(c); break;
				case FieldDecl f: generateFieldClients(f); break;
				case MethodDecl m: generateMethodClients(m); break;
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

		if (!containingClass.isEffectivelyAbstract()) {
			writer.writeConstructorInvocation(it, containingClass);
		}
	}

	private void generateFieldClients(FieldDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		writer.writeFieldRead(it, containingType);

		if (!it.isFinal()) {
			writer.writeFieldWrite(it, containingType);
		}
	}

	private void generateMethodClients(MethodDecl it) {
		var containingTypeOpt = getContainingTypeFromTypeMember(it);
		if (containingTypeOpt.isEmpty()) return;
		var containingType = containingTypeOpt.get();

		if (!containingType.isClass()) return;
		var containingClass = (ClassDecl) containingType;

		writer.writeMethodInvocation(it, containingClass);

		if (!it.isEffectivelyFinal()) { // Checks also if containing class is final or sealed
			writer.writeMethodOverride(it, containingClass);
		}
	}

	private Optional<TypeDecl> getContainingTypeFromTypeMember(TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.getContainingType().getResolvedApiType().isEmpty()) return Optional.empty();

		return Optional.of(typeMemberDecl.getContainingType().getResolvedApiType().get());
	}
}
