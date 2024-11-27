package com.github.maracas.roseau.combinatorial.client.visit;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;
import com.github.maracas.roseau.combinatorial.client.ClientWriter;

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
				default: break;
			}
		}

		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	private void generateClassClients(ClassDecl it) {
		writer.writeTypeReference(it);

		if (!it.isEffectivelyFinal()) {
			writer.writeClassInheritance(it);
		}
	}

	private void generateInterfaceClients(InterfaceDecl it) {
		writer.writeTypeReference(it);
		writer.writeInterfaceExtension(it);
		writer.writeInterfaceImplementation(it);
	}

	private void generateConstructorClients(ConstructorDecl it) {
		writer.writeConstructorInvocation(it);
	}

	private void generateFieldClients(FieldDecl it) {
		writer.writeFieldRead(it);

		if (!it.isFinal()) {
			writer.writeFieldWrite(it);
		}
	}
}
