package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;

final class AsmTypeSignatureVisitor<T extends ITypeReference> extends SignatureVisitor {
	private final TypeReferenceFactory factory;
	private final char wildcard;
	private ITypeReference type;
	private final List<AsmTypeSignatureVisitor<?>> typeArgumentVisitors = new ArrayList<>();
	private AsmTypeSignatureVisitor<?> arrayComponentVisitor;
	private boolean finalized;

	AsmTypeSignatureVisitor(int api, TypeReferenceFactory factory) {
		this(api, factory, INSTANCEOF);
	}

	private AsmTypeSignatureVisitor(int api, TypeReferenceFactory factory, char wildcard) {
		super(api);
		this.factory = factory;
		this.wildcard = wildcard;
	}

	@Override
	public void visitTypeVariable(String name) {
		type = factory.createTypeParameterReference(name);
	}

	@Override
	public void visitClassType(String name) {
		type = factory.createTypeReference(name.replace('/', '.'));
	}

	@Override
	public void visitBaseType(char descriptor) {
		type = switch (descriptor) {
			case 'V' -> factory.createPrimitiveTypeReference("void");
			case 'B' -> factory.createPrimitiveTypeReference("byte");
			case 'J' -> factory.createPrimitiveTypeReference("long");
			case 'Z' -> factory.createPrimitiveTypeReference("boolean");
			case 'I' -> factory.createPrimitiveTypeReference("int");
			case 'S' -> factory.createPrimitiveTypeReference("short");
			case 'C' -> factory.createPrimitiveTypeReference("char");
			case 'F' -> factory.createPrimitiveTypeReference("float");
			case 'D' -> factory.createPrimitiveTypeReference("double");
			default -> throw new RoseauException("Unexpected base type descriptor: " + descriptor);
		};
	}

	@Override
	public SignatureVisitor visitArrayType() {
		arrayComponentVisitor = new AsmTypeSignatureVisitor<>(api, factory);
		return arrayComponentVisitor;
	}

	@Override
	public void visitInnerClassType(String name) {
		if (type != null) {
			// Discard outer class type arguments; they're replaced by inner class
			typeArgumentVisitors.clear();
			type = factory.createTypeReference(type.getQualifiedName() + "$" + name);
		}
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		AsmTypeSignatureVisitor<ITypeReference> visitor = new AsmTypeSignatureVisitor<>(api, factory, wildcard);
		typeArgumentVisitors.add(visitor);
		return visitor;
	}

	@Override
	public void visitTypeArgument() {
		// Unbounded wildcard: ? extends Object
		typeArgumentVisitors.add(createUnboundedWildcard());
	}

	private AsmTypeSignatureVisitor<ITypeReference> createUnboundedWildcard() {
		AsmTypeSignatureVisitor<ITypeReference> visitor = new AsmTypeSignatureVisitor<>(api, factory, INSTANCEOF);
		visitor.type = factory.createWildcardTypeReference(List.of(TypeReference.OBJECT), true);
		visitor.finalized = true;
		return visitor;
	}

	@Override
	public void visitEnd() {
		finalizeType();
	}

	private void finalizeType() {
		if (finalized) {
			return;
		}
		finalized = true;

		// Handle array types
		if (arrayComponentVisitor != null) {
			ITypeReference componentType = arrayComponentVisitor.getType();
			type = (componentType instanceof ArrayTypeReference arrayRef)
				? factory.createArrayTypeReference(arrayRef.componentType(), arrayRef.dimension() + 1)
				: factory.createArrayTypeReference(componentType, 1);
		}

		// Apply type arguments to parameterize the current type
		if (!typeArgumentVisitors.isEmpty()) {
			List<ITypeReference> typeArgs = typeArgumentVisitors.stream()
				.<ITypeReference>map(AsmTypeSignatureVisitor::getType)
				.toList();
			type = factory.createTypeReference(type.getQualifiedName(), typeArgs);
		}

		// Wrap in wildcard if needed (e.g., "? extends List<String>")
		if (wildcard != INSTANCEOF) {
			type = factory.createWildcardTypeReference(List.of(type), wildcard == EXTENDS);
		}
	}

	@SuppressWarnings("unchecked")
	T getType() {
		// Finalize type if not already done (visitEnd may not have been called)
		if (!finalized) {
			finalizeType();
		}
		return (T) type;
	}
}
