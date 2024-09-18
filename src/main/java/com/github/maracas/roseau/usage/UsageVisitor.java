package com.github.maracas.roseau.usage;

import com.github.maracas.roseau.api.SpoonUtils;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsageVisitor extends CtScanner {
	private final API api;
	private final List<Use> uses = new ArrayList<>();

	public UsageVisitor(API api) {
		this.api = api;
	}

	@Override
	public <T> void visitCtInvocation(CtInvocation<T> invocation) {
		super.visitCtInvocation(invocation);
		findAPISymbol(invocation.getExecutable()).ifPresent(
			s -> {
				if (invocation.getExecutable().isConstructor()) {
					uses.add(new Use(s.getContainingType().getResolvedApiType().get(),
						UseType.INSTANTIATION, SpoonUtils.convertSpoonPosition(invocation.getPosition())));
				}

				uses.add(new Use(s, UseType.INVOCATION, SpoonUtils.convertSpoonPosition(invocation.getPosition())));
			}
		);
	}

	@Override
	public <T> void visitCtLambda(CtLambda<T> lambda) {
		super.visitCtLambda(lambda);
		if (lambda.getOverriddenMethod() != null)
			findAPISymbol(lambda.getOverriddenMethod().getReference()).ifPresent(
				s -> uses.add(new Use(s, UseType.OVERRIDE, SpoonUtils.convertSpoonPosition(lambda.getPosition())))
			);
	}

	@Override
	public <T> void visitCtTypeReference(CtTypeReference<T> typeReference) {
		super.visitCtTypeReference(typeReference);
		findAPISymbol(typeReference).ifPresent(
			s -> uses.add(new Use(s, UseType.REFERENCE, SpoonUtils.convertSpoonPosition(typeReference.getPosition())))
		);
	}

	@Override
	public <T> void visitCtClass(CtClass<T> cls) {
		super.visitCtClass(cls);
		if (cls.getSuperclass() != null) {
			findAPISymbol(cls.getSuperclass()).ifPresent(
				s -> uses.add(new Use(s, UseType.INHERITANCE, SpoonUtils.convertSpoonPosition(cls.getPosition())))
			);
		}

		cls.getSuperInterfaces().forEach(intf -> {
			findAPISymbol(intf).ifPresent(
				s -> uses.add(new Use(s, UseType.IMPLEMENTATION, SpoonUtils.convertSpoonPosition(cls.getPosition())))
			);
		});
	}

	@Override
	public <T> void visitCtFieldRead(CtFieldRead<T> fieldRead) {
		super.visitCtFieldRead(fieldRead);

		findAPISymbol(fieldRead.getVariable()).ifPresent(
			s -> uses.add(new Use(s, UseType.FIELD_READ, SpoonUtils.convertSpoonPosition(fieldRead.getPosition())))
		);
	}

	@Override
	public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite) {
		super.visitCtFieldWrite(fieldWrite);

		findAPISymbol(fieldWrite.getVariable()).ifPresent(
			s -> uses.add(new Use(s, UseType.FIELD_WRITE, SpoonUtils.convertSpoonPosition(fieldWrite.getPosition())))
		);
	}

	<T> Optional<TypeDecl> findAPISymbol(CtTypeReference<T> typeReference) {
		return api.findType(typeReference.getQualifiedName());
	}

	<T> Optional<MethodDecl> findAPISymbol(CtExecutableReference<T> executable) {
		if (executable.getDeclaringType() != null) {
			Optional<TypeDecl> type = findAPISymbol(executable.getDeclaringType());

			if (type.isPresent()) {
				return type.get().findMethod(executable.getSignature());
			}
		}
		return Optional.empty();
	}


	<T> Optional<FieldDecl> findAPISymbol(CtFieldReference<T> fieldReference) {
		if (fieldReference.getDeclaringType() != null) {
			Optional<TypeDecl> type = findAPISymbol(fieldReference.getDeclaringType());

			if (type.isPresent()) {
				return type.get().findField(fieldReference.getSimpleName());
			}
		}
		return Optional.empty();
	}

	public List<Use> getUses() {
		return uses;
	}
}
