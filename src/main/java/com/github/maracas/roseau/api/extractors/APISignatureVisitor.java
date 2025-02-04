package com.github.maracas.roseau.api.extractors;

import com.github.maracas.roseau.api.model.FormalTypeParameter;
import com.github.maracas.roseau.api.model.reference.ITypeReference;
import com.github.maracas.roseau.api.model.reference.TypeReferenceFactory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

public class APISignatureVisitor extends SignatureVisitor {
	private final TypeReferenceFactory typeRefFactory;
	private List<FormalTypeParameter> formalTypeParameters = new ArrayList<>();
	private String currentTypeParameter;
	private List<ITypeReference> currentBounds;
	ITypeReference type;

	public APISignatureVisitor(int api, TypeReferenceFactory typeRefFactory) {
		super(api);
		this.typeRefFactory = typeRefFactory;
	}

	private String internalToFqn(String internalName) {
		return internalName.replace('/', '.');
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		if (currentTypeParameter != null && currentBounds != null) {
			formalTypeParameters.add(new FormalTypeParameter(currentTypeParameter, currentBounds));
		}

		// Start a new type parameter
		currentTypeParameter = name;
		currentBounds = new ArrayList<>();
	}

	@Override
	public SignatureVisitor visitClassBound() {
		return new TypeReferenceCollector(api, currentBounds);
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		return new TypeReferenceCollector(api, currentBounds);
	}

	@Override
	public SignatureVisitor visitReturnType() {
		return new SignatureVisitor(api) {
			String returnType = null;
			List<ITypeReference> bounds = new ArrayList();

			@Override
			public void visitClassType(String name) {
				returnType = name;
			}

			@Override
			public void visitTypeVariable(final String name) {
				bounds.add(typeRefFactory.createTypeParameterReference(internalToFqn(name)));
			}

			@Override
			public void visitEnd() {
				type = typeRefFactory.createTypeReference(
					internalToFqn(returnType),
					bounds
				);
			}
		};
	}

	@Override
	public void visitEnd() {
		// Store the collected type parameter
		if (currentTypeParameter != null && currentBounds != null) {
			formalTypeParameters.add(new FormalTypeParameter(currentTypeParameter, currentBounds));
		}
	}

	public List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameters;
	}

	public ITypeReference getType() {
		return type;
	}

	// Helper class to collect type references
	private class TypeReferenceCollector extends SignatureVisitor {
		private final List<ITypeReference> bounds;

		public TypeReferenceCollector(int api, List<ITypeReference> bounds) {
			super(api);
			this.bounds = bounds;
		}

		@Override
		public void visitClassType(String name) {
			bounds.add(typeRefFactory.createTypeReference(internalToFqn(name)));
		}

		@Override
		public void visitTypeVariable(final String name) {
			bounds.add(typeRefFactory.createTypeParameterReference(name));
		}
	}
}