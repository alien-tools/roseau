package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.IntStream;

final class AsmSignatureVisitor extends SignatureVisitor {
	private final TypeReferenceFactory typeRefFactory;
	private AsmTypeSignatureVisitor<TypeReference<ClassDecl>> superClassVisitor;
	private AsmTypeSignatureVisitor<ITypeReference> returnTypeVisitor;
	private final List<AsmTypeSignatureVisitor<TypeReference<InterfaceDecl>>> interfaceVisitors = new ArrayList<>();
	private final List<AsmTypeSignatureVisitor<ITypeReference>> parameterVisitors = new ArrayList<>();
	private final List<AsmTypeSignatureVisitor<ITypeReference>> thrownVisitors = new ArrayList<>();
	// preserve visit ordering
	private final SequencedMap<String, List<AsmTypeSignatureVisitor<ITypeReference>>> formalTypeParameterVisitors =
		new LinkedHashMap<>();

	AsmSignatureVisitor(int api, TypeReferenceFactory typeRefFactory) {
		super(api);
		this.typeRefFactory = typeRefFactory;
	}

	List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameterVisitors.entrySet().stream()
			.map(e -> new FormalTypeParameter(e.getKey(),
				e.getValue().stream().map(AsmTypeSignatureVisitor::getType).toList()
			))
			.toList();
	}

	ITypeReference getReturnType() {
		return returnTypeVisitor != null ? returnTypeVisitor.getType() : null;
	}

	List<ParameterDecl> getParameters() {
		return IntStream.range(0, parameterVisitors.size())
			.mapToObj(i -> new ParameterDecl("p" + i, parameterVisitors.get(i).getType(), false))
			.toList();
	}

	TypeReference<ClassDecl> getSuperclass() {
		return superClassVisitor.getType();
	}

	List<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return interfaceVisitors.stream()
			.map(AsmTypeSignatureVisitor::getType)
			.toList();
	}

	List<ITypeReference> getThrownExceptions() {
		return thrownVisitors.stream()
			.map(AsmTypeSignatureVisitor::getType)
			.toList();
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		formalTypeParameterVisitors.putLast(name, new ArrayList<>());
	}

	@Override
	public SignatureVisitor visitClassBound() {
		var v = new AsmTypeSignatureVisitor<>(api, typeRefFactory);
		formalTypeParameterVisitors.lastEntry().getValue().add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		var v = new AsmTypeSignatureVisitor<>(api, typeRefFactory);
		formalTypeParameterVisitors.lastEntry().getValue().add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		var v = new AsmTypeSignatureVisitor<TypeReference<ClassDecl>>(api, typeRefFactory);
		superClassVisitor = v;
		return v;
	}

	@Override
	public SignatureVisitor visitInterface() {
		var v = new AsmTypeSignatureVisitor<TypeReference<InterfaceDecl>>(api, typeRefFactory);
		interfaceVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		var v = new AsmTypeSignatureVisitor<>(api, typeRefFactory);
		parameterVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitReturnType() {
		var v = new AsmTypeSignatureVisitor<>(api, typeRefFactory);
		returnTypeVisitor = v;
		return v;
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		var v = new AsmTypeSignatureVisitor<>(api, typeRefFactory);
		thrownVisitors.add(v);
		return v;
	}
}
