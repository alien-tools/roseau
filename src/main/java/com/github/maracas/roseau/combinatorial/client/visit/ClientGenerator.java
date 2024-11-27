package com.github.maracas.roseau.combinatorial.client.visit;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.model.reference.*;
import com.github.maracas.roseau.api.visit.APIAlgebra;
import com.github.maracas.roseau.combinatorial.client.ClientWriter;

public class ClientGenerator implements APIAlgebra<Generate> {
	private final ClientWriter writer;

	public ClientGenerator(ClientWriter writer) {
		this.writer = writer;
	}

	@Override
	public Generate api(API it) {
		return () -> it.getAllTypes().parallel().forEach(type -> this.$(type).generate());
	}

	@Override
	public Generate classDecl(ClassDecl it) {
		return () -> {
			if (!it.isExported()) return;

			writer.writeTypeReference(it);

			if (!it.isEffectivelyFinal()) {
				writer.writeClassInheritance(it);
			}

			if (it.isEffectivelyAbstract()) return;

			it.getConstructors().parallelStream().forEach(ctr -> this.$(ctr).generate());
			it.getAllFields().parallel().forEach(fld -> this.$(fld).generate());
			it.getAllMethods().parallel().forEach(mtd -> this.$(mtd).generate());
		};
	}

	@Override
	public Generate interfaceDecl(InterfaceDecl it) {
		return () -> {
			if (!it.isExported()) return;

			writer.writeTypeReference(it);
			writer.writeInterfaceExtension(it);
			writer.writeInterfaceImplementation(it);

			it.getAllFields().parallel().forEach(fld -> this.$(fld).generate());
			it.getAllMethods().parallel().forEach(mtd -> this.$(mtd).generate());
		};
	}

	@Override
	public Generate enumDecl(EnumDecl it) {
		return classDecl(it);
	}

	@Override
	public Generate recordDecl(RecordDecl it) {
		return classDecl(it);
	}

	@Override
	public Generate constructorDecl(ConstructorDecl it) {
		return () -> writer.writeConstructorInvocation(it);
	}

	@Override
	public Generate methodDecl(MethodDecl it) {
		return () -> {
//			System.out.println("In methodDecl: " + it.toString());
		};
	}

	@Override
	public Generate fieldDecl(FieldDecl it) {
		return () -> {
			writer.writeFieldRead(it);

			if (!it.isFinal()) {
				writer.writeFieldWrite(it);
			}
		};
	}

	@Override
	public Generate annotationDecl(AnnotationDecl it) {
		return () -> {
//			System.out.println("In annotationDecl: " + it.toString());
		};
	}

	@Override
	public Generate parameterDecl(ParameterDecl it) { return () -> {}; }
	@Override
	public Generate annotation(Annotation it) { return () -> {}; }
	@Override
	public <U extends TypeDecl> Generate typeReference(TypeReference<U> it) { return () -> {}; }
	@Override
	public Generate primitiveTypeReference(PrimitiveTypeReference it) { return () -> {}; }
	@Override
	public Generate arrayTypeReference(ArrayTypeReference it) { return () -> {}; }
	@Override
	public Generate typeParameterReference(TypeParameterReference it) { return () -> {}; }
	@Override
	public Generate wildcardTypeReference(WildcardTypeReference it) { return () -> {}; }
	@Override
	public Generate formalTypeParameter(FormalTypeParameter it) { return () -> {}; }
}
