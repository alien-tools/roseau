package com.github.maracas.roseau.api.extractors.jar;

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
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class APISignatureVisitor extends SignatureVisitor {
	TypeReferenceFactory typeRefFactory;
	String currentFormalTypeParameter = null;
	List<ITypeReference> currentBounds = new ArrayList<>();
	ITypeReference returnType = null;
	boolean isBound = false;
	boolean isReturnType = false;
	List<TypeVisitor> interfaceVisitors = new ArrayList<>();
	TypeVisitor returnTypeVisitor = null;
	Map<String, List<TypeVisitor>> formalTypeParameterVisitors = new LinkedHashMap<>(); // preserve ordering
	List<TypeVisitor> currentVisitors = new ArrayList<>();
	List<TypeVisitor> parameterVisitors = new ArrayList<>();
	Supplier<TypeReference<ClassDecl>> superClass = null;
	List<Supplier<ITypeReference>> thrownVisitors = new ArrayList<>();

	APISignatureVisitor(int api, TypeReferenceFactory typeRefFactory) {
		super(api);
		this.typeRefFactory = typeRefFactory;
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
		return IntStream.range(0, parameterVisitors.size())
			.mapToObj(i -> new ParameterDecl(String.format("p%d", i),
				parameterVisitors.get(i).getType(), false))
			.toList();
	}

	public TypeReference<ClassDecl> getSuperclass() {
		return superClass.get();
	}

	public List<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return interfaceVisitors.stream()
			.map(v -> (TypeReference<InterfaceDecl>) v.getType())
			.toList();
	}

	public List<ITypeReference> getThrownExceptions() {
		return thrownVisitors.stream().map(Supplier::get).toList();
	}

	public void endFormalTypeParameter() {
		if (currentFormalTypeParameter != null) {
			formalTypeParameterVisitors.put(currentFormalTypeParameter, currentVisitors);
			currentFormalTypeParameter = null;
			currentVisitors = new ArrayList<>();
		}
	}

	public String internalToFqn(String internalName) {
		return internalName.replace('/', '.');
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		endFormalTypeParameter();
		currentFormalTypeParameter = name;
	}

	@Override
	public SignatureVisitor visitClassBound() {
		var v = new TypeVisitor(api, typeRefFactory);
		currentVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		var v = new TypeVisitor(api, typeRefFactory);
		currentVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		endFormalTypeParameter();
		var v = new TypeVisitor(api, typeRefFactory);
		superClass = () -> (TypeReference<ClassDecl>) v.getType();
		return v;
	}

	@Override
	public SignatureVisitor visitInterface() {
		var v = new TypeVisitor(api, typeRefFactory);
		interfaceVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		endFormalTypeParameter();
		var v = new TypeVisitor(api, typeRefFactory);
		parameterVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitReturnType() {
		endFormalTypeParameter();
		var v = new TypeVisitor(api, typeRefFactory);
		returnTypeVisitor = v;
		return v;
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		var v = new TypeVisitor(api, typeRefFactory);
		thrownVisitors.add(() -> v.getType());
		return v;
	}

	@Override
	public void visitBaseType(char descriptor) {
	}

	@Override
	public void visitTypeVariable(String name) {
		if (isBound) {
			isBound = false;
			currentBounds.add(typeRefFactory.createTypeParameterReference(name));
		}
		if (isReturnType) {
			isReturnType = false;
			returnType = typeRefFactory.createTypeParameterReference(name);
		}
	}

	@Override
	public SignatureVisitor visitArrayType() {
		var v = new TypeVisitor(api, typeRefFactory);
		return v;
	}

	@Override
	public void visitClassType(String name) {
		if (isBound) {
			isBound = false;
			currentBounds.add(typeRefFactory.createTypeReference(internalToFqn(name)));
		}
		if (isReturnType) {
			isReturnType = false;
			returnType = typeRefFactory.createTypeReference(internalToFqn(name));
		}
	}

	@Override
	public void visitInnerClassType(String name) {
	}

	@Override
	public void visitTypeArgument() {
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		return new TypeVisitor(api, typeRefFactory);
	}

	@Override
	public void visitEnd() {
	}

	public static class TypeVisitor extends SignatureVisitor {
		TypeReferenceFactory factory;
		ITypeReference type;
		List<Supplier<ITypeReference>> visitors = new ArrayList<>();
		char currentWildcard = INSTANCEOF;

		TypeVisitor(int api, TypeReferenceFactory factory) {
			super(api);
			this.factory = factory;
		}

		TypeVisitor(int api, TypeReferenceFactory factory, char wildcard) {
			this(api, factory);
			this.currentWildcard = wildcard;
		}

		@Override
		public void visitTypeVariable(String name) {
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
			// Discard what we had already (a visitClassType with possible type args)
			// and just register the inner, most precise type
			visitors.clear();
			visitClassType(type.getQualifiedName() + "$" + name);
			super.visitInnerClassType(name);
		}

		@Override
		public void visitBaseType(char descriptor) {
			switch (descriptor) {
				case 'V': type = factory.createPrimitiveTypeReference("void"); break;
				case 'B': type = factory.createPrimitiveTypeReference("byte"); break;
				case 'J': type = factory.createPrimitiveTypeReference("long"); break;
				case 'Z': type = factory.createPrimitiveTypeReference("boolean"); break;
				case 'I': type = factory.createPrimitiveTypeReference("int"); break;
				case 'S': type = factory.createPrimitiveTypeReference("short"); break;
				case 'C': type = factory.createPrimitiveTypeReference("char"); break;
				case 'F': type = factory.createPrimitiveTypeReference("float"); break;
				default: type = factory.createPrimitiveTypeReference("double"); break;
			}
		}

		@Override
		public SignatureVisitor visitArrayType() {
			TypeVisitor visitor = new TypeVisitor(api, factory);
			visitors.add(() ->
				factory.createArrayTypeReference(visitor.getType(), 1));
			return visitor;
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			TypeVisitor visitor = new TypeVisitor(api, factory, wildcard);
			visitors.add(() -> visitor.getType());
			return visitor;
		}

		@Override
		public void visitTypeArgument() { // If this is called, it's an unbounded wildcard
			visitors.add(() -> factory.createWildcardTypeReference(List.of(TypeReference.OBJECT), true));
		}

		@Override
		public void visitEnd() {
			if (!visitors.isEmpty()) {
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
