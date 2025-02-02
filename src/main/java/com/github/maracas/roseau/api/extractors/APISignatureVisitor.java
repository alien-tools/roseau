package com.github.maracas.roseau.api.extractors;

import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

public class APISignatureVisitor extends SignatureVisitor {
	private final StringBuilder stringBuilder;
	private final List<String> typeArguments = new ArrayList<>();
	private boolean hasFormals;
	private boolean hasParameters;
	private int argumentStack;

	public APISignatureVisitor() {
		this(new StringBuilder());
	}

	private APISignatureVisitor(StringBuilder stringBuilder) {
		super(589824);
		this.argumentStack = 1;
		this.stringBuilder = stringBuilder;
	}

	public void visitFormalTypeParameter(String name) {
		if (!this.hasFormals) {
			this.hasFormals = true;
			this.stringBuilder.append('<');
		}

		this.stringBuilder.append(name);
		this.stringBuilder.append(':');
	}

	public SignatureVisitor visitClassBound() {
		return this;
	}

	public SignatureVisitor visitInterfaceBound() {
		this.stringBuilder.append(':');
		return this;
	}

	public SignatureVisitor visitSuperclass() {
		this.endFormals();
		return this;
	}

	public SignatureVisitor visitInterface() {
		return this;
	}

	public SignatureVisitor visitParameterType() {
		this.endFormals();
		if (!this.hasParameters) {
			this.hasParameters = true;
			this.stringBuilder.append('(');
		}

		return this;
	}

	public SignatureVisitor visitReturnType() {
		this.endFormals();
		if (!this.hasParameters) {
			this.stringBuilder.append('(');
		}

		this.stringBuilder.append(')');
		return this;
	}

	public SignatureVisitor visitExceptionType() {
		this.stringBuilder.append('^');
		return this;
	}

	public void visitBaseType(char descriptor) {
		this.stringBuilder.append(descriptor);
	}

	public void visitTypeVariable(String name) {
		this.stringBuilder.append('T');
		this.stringBuilder.append(name);
		this.stringBuilder.append(';');
	}

	public SignatureVisitor visitArrayType() {
		this.stringBuilder.append('[');
		return this;
	}

	public void visitClassType(String name) {
		this.stringBuilder.append('L');
		this.stringBuilder.append(name);
		this.typeArguments.add(name);
		this.argumentStack <<= 1;
	}

	public void visitInnerClassType(String name) {
		this.endArguments();
		this.stringBuilder.append('.');
		this.stringBuilder.append(name);
		this.argumentStack <<= 1;
	}

	public void visitTypeArgument() {
		if ((this.argumentStack & 1) == 0) {
			this.argumentStack |= 1;
			this.stringBuilder.append('<');
		}

		this.stringBuilder.append('*');
	}

	public SignatureVisitor visitTypeArgument(char wildcard) {
		if ((this.argumentStack & 1) == 0) {
			this.argumentStack |= 1;
			this.stringBuilder.append('<');
		}

		if (wildcard != '=') {
			this.stringBuilder.append(wildcard);
		}

		return (this.argumentStack & Integer.MIN_VALUE) == 0 ? this : new APISignatureVisitor(this.stringBuilder);
	}

	public void visitEnd() {
		this.endArguments();
		this.stringBuilder.append(';');
	}

	public List<String> getTypeArguments() {
		// First is the receiving type
		return typeArguments.stream().skip(1).toList();
	}

	public String toString() {
		return this.stringBuilder.toString();
	}

	private void endFormals() {
		if (this.hasFormals) {
			this.hasFormals = false;
			this.stringBuilder.append('>');
		}

	}

	private void endArguments() {
		if ((this.argumentStack & 1) == 1) {
			this.stringBuilder.append('>');
		}

		this.argumentStack >>>= 1;
	}
}
