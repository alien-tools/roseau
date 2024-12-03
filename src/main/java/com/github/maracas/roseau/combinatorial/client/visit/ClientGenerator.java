package com.github.maracas.roseau.combinatorial.client.visit;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;
import com.github.maracas.roseau.combinatorial.client.ClientWriter;

import java.util.Optional;

public class ClientGenerator extends AbstractAPIVisitor {
	private final ClientWriter writer;

	public ClientGenerator(ClientWriter writer) {
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

		if (it.isEffectivelyFinal()) return;

		writer.writeClassInheritance(it);
	}

	private void generateInterfaceClients(InterfaceDecl it) {
		writer.writeTypeReference(it);
		writer.writeInterfaceExtension(it);
		writer.writeInterfaceImplementation(it);
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
		var originalClassOpt = getOriginClassFromTypeMember(it);
		if (originalClassOpt.isEmpty()) return;

		var originalClass = originalClassOpt.get();
		if (originalClass.isEffectivelyAbstract() && !it.isStatic()) return;

		writer.writeFieldRead(it, originalClass);

		if (it.isFinal()) return;

		writer.writeFieldWrite(it, originalClass);
	}

	private void generateMethodClients(MethodDecl it) {
		var originalClassOpt = getOriginClassFromTypeMember(it);
		if (originalClassOpt.isEmpty()) return;

		var originalClass = originalClassOpt.get();
		if (originalClass.isEffectivelyAbstract() && !it.isStatic()) return;

		writer.writeMethodInvocation(it, originalClass);

		if (originalClass.isEffectivelyFinal()) return;

		writer.writeMethodOverride(it, originalClass);
	}

	private Optional<TypeDecl> getContainingTypeFromTypeMember(TypeMemberDecl typeMemberDecl) {
		if (typeMemberDecl.getContainingType().getResolvedApiType().isEmpty()) return Optional.empty();

		return Optional.of(typeMemberDecl.getContainingType().getResolvedApiType().get());
	}
}
