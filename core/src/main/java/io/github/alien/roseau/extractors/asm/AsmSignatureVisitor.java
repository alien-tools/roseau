package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.ParameterDecl;
import io.github.alien.roseau.api.model.factory.ApiFactory;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

final class AsmSignatureVisitor extends SignatureVisitor {
	private final ApiFactory factory;
	private AsmTypeSignatureVisitor<TypeReference<ClassDecl>> superClassVisitor;
	private AsmTypeSignatureVisitor<ITypeReference> returnTypeVisitor;
	private final List<AsmTypeSignatureVisitor<TypeReference<InterfaceDecl>>> interfaceVisitors = new ArrayList<>();
	private final List<AsmTypeSignatureVisitor<ITypeReference>> parameterVisitors = new ArrayList<>();
	private final List<AsmTypeSignatureVisitor<ITypeReference>> thrownVisitors = new ArrayList<>();
	// LinkedHashMap preserves insertion order for type parameters
	private final SequencedMap<String, List<AsmTypeSignatureVisitor<ITypeReference>>> typeParameterBounds =
		new LinkedHashMap<>();

	AsmSignatureVisitor(int api, ApiFactory factory) {
		super(api);
		this.factory = factory;
	}

	List<FormalTypeParameter> getFormalTypeParameters() {
		return typeParameterBounds.entrySet().stream()
			.map(e -> factory.createFormalTypeParameter(e.getKey(),
				e.getValue().stream().map(AsmTypeSignatureVisitor::getType).toList()
			))
			.toList();
	}

	ITypeReference getReturnType() {
		return returnTypeVisitor != null ? returnTypeVisitor.getType() : null;
	}

	List<ParameterDecl> getParameters() {
		List<ParameterDecl> parameters = new ArrayList<>();
		for (int i = 0; i < parameterVisitors.size(); i++) {
			parameters.add(new ParameterDecl("p" + i, parameterVisitors.get(i).getType(), false));
		}
		return parameters;
	}

	TypeReference<ClassDecl> getSuperclass() {
		return superClassVisitor.getType();
	}

	Set<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return interfaceVisitors.stream()
			.map(AsmTypeSignatureVisitor::getType)
			.collect(toSet());
	}

	Set<ITypeReference> getThrownExceptions() {
		return thrownVisitors.stream()
			.map(AsmTypeSignatureVisitor::getType)
			.collect(toSet());
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		typeParameterBounds.putLast(name, new ArrayList<>());
	}

	@Override
	public SignatureVisitor visitClassBound() {
		return addBoundVisitor();
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		return addBoundVisitor();
	}

	private SignatureVisitor addBoundVisitor() {
		AsmTypeSignatureVisitor<ITypeReference> visitor = new AsmTypeSignatureVisitor<>(api, factory.references());
		typeParameterBounds.lastEntry().getValue().add(visitor);
		return visitor;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		return superClassVisitor = new AsmTypeSignatureVisitor<>(api, factory.references());
	}

	@Override
	public SignatureVisitor visitInterface() {
		return addVisitorToList(interfaceVisitors);
	}

	@Override
	public SignatureVisitor visitParameterType() {
		return addVisitorToList(parameterVisitors);
	}

	@Override
	public SignatureVisitor visitReturnType() {
		return returnTypeVisitor = new AsmTypeSignatureVisitor<>(api, factory.references());
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		return addVisitorToList(thrownVisitors);
	}

	private <V extends ITypeReference> AsmTypeSignatureVisitor<V> addVisitorToList(List<? super AsmTypeSignatureVisitor<V>> list) {
		AsmTypeSignatureVisitor<V> visitor = new AsmTypeSignatureVisitor<>(api, factory.references());
		list.add(visitor);
		return visitor;
	}
}
