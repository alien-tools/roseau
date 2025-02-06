package com.github.maracas.roseau.api.extractors;

import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.ParameterDecl;
import com.github.maracas.roseau.api.model.reference.ArrayTypeReference;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import com.github.maracas.roseau.api.model.reference.WildcardTypeReference;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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
	boolean isBound = false;
	boolean isReturnType = false;
	String prefix = "";
	TypeReference<ClassDecl> superClass = null;
	List<TypeReference<InterfaceDecl>> superInterfaces = new ArrayList<>();
	List<TypeVisitor> interfaceVisitors = new ArrayList<>();
	TypeVisitor returnTypeVisitor = null;
	TypeVisitor superClassVisitor = null;
	Map<String, List<TypeVisitor>> formalTypeParameterVisitors = new LinkedHashMap<>(); // preserve ordering
	List<TypeVisitor> currentVisitors = new ArrayList<>();
	List<TypeVisitor> parameterVisitors = new ArrayList<>();
	boolean isVarargs;

	APISignatureVisitor(int api, TypeReferenceFactory factory, boolean isVarargs, String prefix) {
		super(api);
		this.factory = factory;
		this.prefix = prefix;
		this.isVarargs = isVarargs;
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameterVisitors.entrySet().stream()
			.map(e -> new FormalTypeParameter(e.getKey(),
				e.getValue().stream()
					.map(TypeVisitor::getType)
					.toList()
			))
			.toList();
	}

	public ITypeReference getReturnType() {
		if (returnTypeVisitor != null)
			return returnTypeVisitor.getType();
		return null;
	}

	public List<ParameterDecl> getParameters() {
		var ret = new ArrayList<ParameterDecl>();
		for (int i = 0; i < parameterVisitors.size(); i++) {
			var t = parameterVisitors.get(i).getType();
			if (i == parameterVisitors.size() - 1 && isVarargs && t instanceof ArrayTypeReference atr)
				ret.add(new ParameterDecl("p", atr.componentType(), true));
			else
				ret.add(new ParameterDecl("p", t, false));
		}
		return ret;
	}

	public TypeReference<ClassDecl> getSuperclass() {
		return (TypeReference<ClassDecl>) superClassVisitor.getType();
	}

	public List<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return interfaceVisitors.stream()
			.map(v -> (TypeReference<InterfaceDecl>) v.getType())
			.toList();
	}

	public void endFormalTypeParameter() {
		if (currentFormalTypeParameter != null) {
			System.out.println("Saving ftp " + currentFormalTypeParameter + "[" + currentVisitors.stream().map(TypeVisitor::getType).toList() + "]");
			formalTypeParameterVisitors.put(currentFormalTypeParameter, currentVisitors);
			currentFormalTypeParameter = null;
			currentVisitors = new ArrayList<>();
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
		var v = new TypeVisitor(api, factory, isVarargs, "");
		currentVisitors.add(v);
		return v;
	}

	public SignatureVisitor visitInterfaceBound() {
		System.out.println(prefix + "visitInterfaceBound()");
		var v = new TypeVisitor(api, factory, isVarargs, "");
		currentVisitors.add(v);
		return v;
	}

	public SignatureVisitor visitSuperclass() {
		endFormalTypeParameter();
		System.out.println(prefix + "visitSuperclass()");
		superClassVisitor = new TypeVisitor(api, factory, isVarargs, "");
		return superClassVisitor;
	}

	public SignatureVisitor visitInterface() {
		System.out.println(prefix + "visitInterface()");
		var v = new TypeVisitor(api, factory, isVarargs, "");
		interfaceVisitors.add(v);
		return v;
	}

	public SignatureVisitor visitParameterType() {
		System.out.println(prefix + "visitParameterType()");
		endFormalTypeParameter();
		var v = new TypeVisitor(api, factory, isVarargs, "");
		parameterVisitors.add(v);
		return v;
	}

	public SignatureVisitor visitReturnType() {
		System.out.println(prefix + "visitReturnType()");
		endFormalTypeParameter();
		var v = new TypeVisitor(api, factory, isVarargs, "");
		returnTypeVisitor = v;
		return v;
	}

	public SignatureVisitor visitExceptionType() {
		System.out.println(prefix + "visitExceptionType()");
		return this;
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
		var v = new TypeVisitor(api, factory, isVarargs, "");
		return v;
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
		return new TypeVisitor(api, factory, isVarargs, prefix);
	}

	public void visitEnd() {
		System.out.println(prefix + "visitEnd()");
	}

	public static class TypeVisitor extends SignatureVisitor {
		TypeReferenceFactory factory;
		ITypeReference type;
		List<ITypeReference> currentTypeArguments = new ArrayList<>();
		String prefix;
		List<Supplier<ITypeReference>> visitors = new ArrayList<>();
		char currentWildcard = INSTANCEOF;
		boolean isVarargs = false;

		TypeVisitor(int api, TypeReferenceFactory factory, boolean isVarargs, String prefix) {
			super(api);
			this.factory = factory;
			this.prefix = prefix;
			this.isVarargs = isVarargs;
		}

		TypeVisitor(int api, TypeReferenceFactory factory, char wildcard, boolean isVarargs, String prefix) {
			this(api, factory, isVarargs, prefix);
			this.currentWildcard = wildcard;
			this.isVarargs = isVarargs;
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
		public void visitInnerClassType(String name) {
			System.out.println(prefix + "\tvisitInnerClassType()");
			super.visitInnerClassType(name);
		}

		@Override
		public void visitFormalTypeParameter(String name) {
			System.out.println(prefix + "\tvisitFormalTypeParameter()");
			super.visitFormalTypeParameter(name);
		}

		@Override
		public SignatureVisitor visitClassBound() {
			System.out.println(prefix + "\tvisitClassBound()");
			return super.visitClassBound();
		}

		@Override
		public SignatureVisitor visitInterfaceBound() {
			System.out.println(prefix + "\tvisitInterfaceBound()");
			return super.visitInterfaceBound();
		}

		@Override
		public SignatureVisitor visitSuperclass() {
			System.out.println(prefix + "\tvisitSuperclass()");
			return super.visitSuperclass();
		}

		@Override
		public SignatureVisitor visitInterface() {
			System.out.println(prefix + "\tvisitInterface()");
			return super.visitInterface();
		}

		@Override
		public SignatureVisitor visitParameterType() {
			System.out.println(prefix + "\tvisitParameterType()");
			return super.visitParameterType();
		}

		@Override
		public SignatureVisitor visitReturnType() {
			System.out.println(prefix + "\tvisitReturnType()");
			return super.visitReturnType();
		}

		@Override
		public SignatureVisitor visitExceptionType() {
			System.out.println(prefix + "\tvisitExceptionType()");
			return super.visitExceptionType();
		}

		@Override
		public void visitBaseType(char descriptor) {
			System.out.println(prefix + "\tvisitBaseType("+descriptor+")");
			switch (descriptor) {
				case 'V':
					type = factory.createPrimitiveTypeReference("void");
					break;
				case 'B':
					type = factory.createPrimitiveTypeReference("byte");
					break;
				case 'J':
					type = factory.createPrimitiveTypeReference("long");
					break;
				case 'Z':
					type = factory.createPrimitiveTypeReference("boolean");
					break;
				case 'I':
					type = factory.createPrimitiveTypeReference("int");
					break;
				case 'S':
					type = factory.createPrimitiveTypeReference("short");
					break;
				case 'C':
					type = factory.createPrimitiveTypeReference("char");
					break;
				case 'F':
					type = factory.createPrimitiveTypeReference("float");
					break;
				// case 'D':
				default:
					type = factory.createPrimitiveTypeReference("double");
					break;
			}
		}

		@Override
		public SignatureVisitor visitArrayType() {
			System.out.println(prefix + "\tvisitArrayType()");
			TypeVisitor visitor = new TypeVisitor(api, factory, isVarargs, prefix + "\t");
			visitors.add(() ->
				factory.createArrayTypeReference(visitor.getType(), 1));
			return visitor;
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			System.out.println(prefix + "\tvisitTypeArgument("+wildcard+")");
			TypeVisitor visitor = new TypeVisitor(api, factory, wildcard, isVarargs, prefix + "\t");
			visitors.add(() -> visitor.getType());
			return visitor;
		}

		@Override
		public void visitTypeArgument() { // If this is called, it's an unbounded wildcard
			System.out.println(prefix + "\tvisitTypeArgument()");
//			type = factory.createTypeReference(type.getQualifiedName(), List.of(
//				factory.createWildcardTypeReference(List.of(TypeReference.OBJECT), true)
//			));
			visitors.add(() -> factory.createWildcardTypeReference(List.of(TypeReference.OBJECT), true));
		}

		@Override
		public void visitEnd() {
			System.out.println(prefix + "\tvisitEnd()");
			if (!visitors.isEmpty()) {
				/*if (type instanceof WildcardTypeReference wtr) {
					var newBounds = visitors.stream().map(Supplier::get).toList();
					type = factory.createWildcardTypeReference(List.of(factory.createTypeReference(wtr.bounds().getLast().getQualifiedName(), newBounds)), currentWildcard == EXTENDS);
				} else {
					type = factory.createTypeReference(type.getQualifiedName(), visitors.stream().map(Supplier::get).toList());
				}*/
				if (type instanceof WildcardTypeReference wtr) {
					var newBounds = visitors.stream().map(Supplier::get).toList();
					type = factory.createWildcardTypeReference(List.of(factory.createTypeReference(wtr.bounds().getLast().getQualifiedName(), newBounds)), currentWildcard == EXTENDS);
				} else {
					type = factory.createTypeReference(type.getQualifiedName(), visitors.stream().map(Supplier::get).toList());
				}
			}
		}

		public ITypeReference getType() {
			return type != null ? type : visitors.getFirst().get();
		}
	}
}
