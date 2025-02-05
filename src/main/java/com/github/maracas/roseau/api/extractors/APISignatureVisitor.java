package com.github.maracas.roseau.api.extractors;

import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

public class APISignatureVisitor extends SignatureVisitor {
	TypeReferenceFactory factory;
	String currentFormalTypeParameter = null;
	List<ITypeReference> currentBounds = new ArrayList<>();
	List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	ITypeReference returnType = null;
	ITypeReference currentType = null;
	List<ITypeReference> currentTypeBounds = new ArrayList<>();
	List<ParameterDecl> parameters = new ArrayList<>();
	ITypeReference currentParameterType = null;
	TypeVisitor lastVisitor = null;
	boolean isBound = false;
	boolean isReturnType = false;
	String prefix;

	APISignatureVisitor(int api, TypeReferenceFactory factory, String prefix) {
		super(api);
		this.factory = factory;
		this.prefix = prefix;
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	public ITypeReference getReturnType() {
		if (lastVisitor != null) {
			return lastVisitor.getType();
		}
		return returnType;
	}

	public List<ParameterDecl> getParameters() {
		return parameters;
	}

	public void endFormalTypeParameter() {
		if (currentFormalTypeParameter != null) {
			if (lastVisitor != null) {
				currentBounds.add(lastVisitor.getType());
				lastVisitor = null;
			}
			formalTypeParameters.add(new FormalTypeParameter(currentFormalTypeParameter, List.copyOf(currentBounds)));
		}
		currentFormalTypeParameter = null;
		currentBounds.clear();
	}

	public void endParameterType() {
		if (lastVisitor != null) {
			parameters.add(new ParameterDecl("p", lastVisitor.getType(), false));
			lastVisitor = null;
		} else {
			System.out.println("Should have a visitor");
		}
	}

	public String internalToFqn(String internalName) {
		return internalName.replace('/', '.');
	}

	public void visitFormalTypeParameter(String name) {
		System.out.println(prefix + "visitFormalTypeParameter("+name+")");
		endFormalTypeParameter();
		currentFormalTypeParameter = name;
	}

	public SignatureVisitor visitClassBound() {
		System.out.println(prefix + "visitClassBound()");
		if (lastVisitor != null)
			currentBounds.add(lastVisitor.getType());
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public SignatureVisitor visitInterfaceBound() {
		System.out.println(prefix + "visitInterfaceBound()");
		if (lastVisitor != null)
			currentBounds.add(lastVisitor.getType());
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public SignatureVisitor visitSuperclass() {
		System.out.println(prefix + "visitSuperclass()");
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public SignatureVisitor visitInterface() {
		System.out.println(prefix + "visitInterface()");
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public SignatureVisitor visitParameterType() {
		System.out.println(prefix + "visitParameterType()");
		endFormalTypeParameter();
		endParameterType();
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public SignatureVisitor visitReturnType() {
		System.out.println(prefix + "visitReturnType()");
		endParameterType();
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public SignatureVisitor visitExceptionType() {
		System.out.println(prefix + "visitExceptionType()");
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public void visitBaseType(char descriptor) {
		System.out.println(prefix + "visitBaseType("+descriptor+")");
	}

	public void visitTypeVariable(String name) {
		System.out.println(prefix + "visitTypeVariable("+name+")");
		if (isBound) {
			isBound = false;
			currentBounds.add(factory.createTypeParameterReference(name));
		}
		if (isReturnType) {
			isReturnType = false;
			returnType = factory.createTypeParameterReference(name);
		}
	}

	public SignatureVisitor visitArrayType() {
		System.out.println(prefix + "visitArrayType()");
		lastVisitor = new TypeVisitor(api, factory, "");
		return lastVisitor;
	}

	public void visitClassType(String name) {
		System.out.println(prefix + "visitClassType("+name+")");
		if (isBound) {
			isBound = false;
			currentBounds.add(factory.createTypeReference(internalToFqn(name)));
		}
		if (isReturnType) {
			isReturnType = false;
			returnType = factory.createTypeReference(internalToFqn(name));
		}
	}

	public void visitInnerClassType(String name) {
		System.out.println(prefix + "visitInnerClassType("+name+")");
	}

	public void visitTypeArgument() {
		System.out.println(prefix + "visitTypeArgument()");
	}

	public SignatureVisitor visitTypeArgument(char wildcard) {
		System.out.println(prefix + "visitTypeArgument("+wildcard+")");
		return new TypeVisitor(api, factory, prefix);
	}

	public void visitEnd() {
		System.out.println(prefix + "visitEnd()");
	}
}

class TypeVisitor extends SignatureVisitor {
	TypeReferenceFactory factory;
	ITypeReference type;
	List<ITypeReference> currentTypeArguments = new ArrayList<>();
	String prefix;
	List<TypeVisitor> visitors = new ArrayList<>();
	char currentWildcard = INSTANCEOF;

	TypeVisitor(int api, TypeReferenceFactory factory, String prefix) {
		super(api);
		this.factory = factory;
		this.prefix = prefix;
	}

	TypeVisitor(int api, TypeReferenceFactory factory, char wildcard, String prefix) {
		this(api, factory, prefix);
		this.currentWildcard = wildcard;
	}

	@Override
	public void visitTypeVariable(String name) {
		System.out.println(prefix + "\tvisitTypeVariable("+name+")("+ currentWildcard +")");
		ITypeReference current = factory.createTypeParameterReference(name);
		if (currentWildcard == INSTANCEOF)
			type = current;
		else if (currentWildcard == SUPER)
			type = factory.createWildcardTypeReference(List.of(current), false);
		else if (currentWildcard == EXTENDS)
			type = factory.createWildcardTypeReference(List.of(current), true);
	}

	@Override
	public void visitClassType(String name) {
		System.out.println(prefix + "\tvisitClassType("+name+")("+ currentWildcard +")");
		ITypeReference current = factory.createTypeReference(name.replace('/', '.'));
		if (currentWildcard == INSTANCEOF)
			type = current;
		else if (currentWildcard == SUPER)
			type = factory.createWildcardTypeReference(List.of(current), false);
		else if (currentWildcard == EXTENDS)
			type = factory.createWildcardTypeReference(List.of(current), true);
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		System.out.println(prefix + "\tvisitTypeArgument("+wildcard+")");
		TypeVisitor visitor = new TypeVisitor(api, factory, wildcard, prefix + "\t");
		visitors.add(visitor);
		return visitor;
	}

	@Override
	public void visitEnd() {
		System.out.println(prefix + "\tvisitEnd()");
		if (!visitors.isEmpty()) {
			type = factory.createTypeReference(type.getQualifiedName(), visitors.stream().map(TypeVisitor::getType).toList());
		}
	}

	public ITypeReference getType() {
		return type;
	}
}
