package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.TypeReferenceFactory;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class AsmTypeSignatureVisitor<T extends ITypeReference> extends SignatureVisitor {
	private final TypeReferenceFactory factory;
	private ITypeReference type;
	private final List<Supplier<ITypeReference>> visitors = new ArrayList<>();
	private char wildcard = INSTANCEOF;

	AsmTypeSignatureVisitor(int api, TypeReferenceFactory factory) {
		super(api);
		this.factory = factory;
	}

	AsmTypeSignatureVisitor(int api, TypeReferenceFactory factory, char wildcard) {
		this(api, factory);
		this.wildcard = wildcard;
	}

	@Override
	public void visitTypeVariable(String name) {
		type = wrapWildcard(factory.createTypeParameterReference(name));
	}

	@Override
	public void visitClassType(String name) {
		String typeName = name.replace('/', '.');
		type = wrapWildcard(factory.createTypeReference(typeName));
	}

	@Override
	public SignatureVisitor visitArrayType() {
		AsmTypeSignatureVisitor<ITypeReference> visitor = new AsmTypeSignatureVisitor<>(api, factory);
		visitors.add(() -> {
			// If we've got an array, just increment the dimension
			ITypeReference arrayType =
				visitor.getType() instanceof ArrayTypeReference(ITypeReference componentType, int dimension)
					? factory.createArrayTypeReference(componentType, dimension + 1)
					: factory.createArrayTypeReference(visitor.getType(), 1);

			return wrapWildcard(arrayType);
		});
		return visitor;
	}

	// If we're currently building a wildcard (wildcard != INSTANCEOF), wrap it
	private ITypeReference wrapWildcard(ITypeReference wrapped) {
		return switch (wildcard) {
			case INSTANCEOF -> wrapped;
			case SUPER -> factory.createWildcardTypeReference(List.of(wrapped), false);
			case EXTENDS -> factory.createWildcardTypeReference(List.of(wrapped), true);
			default -> throw new IllegalStateException("ASM is drunk");
		};
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
			default -> factory.createPrimitiveTypeReference("double");
		};
	}

	@Override
	public void visitInnerClassType(String name) {
		// Discard what we had already (a visitClassType with possible type args that are irrelevant)
		// and just register the inner, most precise type
		visitors.clear();
		visitClassType(type.getQualifiedName() + "$" + name);
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		AsmTypeSignatureVisitor<ITypeReference> visitor = new AsmTypeSignatureVisitor<>(api, factory, wildcard);
		visitors.add(visitor::getType);
		return visitor;
	}

	@Override
	public void visitTypeArgument() {
		// If this is called, it's an unbounded wildcard
		visitors.add(() -> factory.createWildcardTypeReference(List.of(TypeReference.OBJECT), true));
	}

	@Override
	public void visitEnd() {
		if (!visitors.isEmpty()) {
			if (type instanceof WildcardTypeReference wtr) {
				// We need to attach current bounds to the existing wildcard
				var newBounds = visitors.stream().map(Supplier::get).toList();
				// FIXME: this getLast() won't last
				type = factory.createWildcardTypeReference(
					List.of(factory.createTypeReference(wtr.bounds().getLast().getQualifiedName(), newBounds)),
					wildcard == EXTENDS);
			} else {
				// We attach the collected bounds to the current type
				type = factory.createTypeReference(type.getQualifiedName(), visitors.stream().map(Supplier::get).toList());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public T getType() {
		// prayge
		return (T) (type != null ? type : visitors.getFirst().get());
	}
}
