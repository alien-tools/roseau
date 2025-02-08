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
import java.util.function.Supplier;
import java.util.stream.IntStream;

class APISignatureVisitor extends SignatureVisitor {
	TypeReferenceFactory typeRefFactory;

	TypeVisitor<TypeReference<ClassDecl>> superClassVisitor = null;
	TypeVisitor<ITypeReference> returnTypeVisitor = null;
	List<TypeVisitor<TypeReference<InterfaceDecl>>> interfaceVisitors = new ArrayList<>();
	List<TypeVisitor<ITypeReference>> parameterVisitors = new ArrayList<>();
	List<TypeVisitor<ITypeReference>> thrownVisitors = new ArrayList<>();
	// preserve visit ordering
	LinkedHashMap<String, List<TypeVisitor<ITypeReference>>> formalTypeParameterVisitors = new LinkedHashMap<>();

	APISignatureVisitor(int api, TypeReferenceFactory typeRefFactory) {
		super(api);
		this.typeRefFactory = typeRefFactory;
	}

	List<FormalTypeParameter> getFormalTypeParameters() {
		return formalTypeParameterVisitors.entrySet().stream()
			.map(e -> new FormalTypeParameter(e.getKey(),
				e.getValue().stream()
					.map(TypeVisitor::getType)
					.toList()
			))
			.toList();
	}

	ITypeReference getReturnType() {
		return returnTypeVisitor != null ? returnTypeVisitor.getType() : null;
	}

	List<ParameterDecl> getParameters() {
		return IntStream.range(0, parameterVisitors.size())
			.mapToObj(i -> new ParameterDecl(String.format("p%d", i),
				parameterVisitors.get(i).getType(), false))
			.toList();
	}

	TypeReference<ClassDecl> getSuperclass() {
		return superClassVisitor.getType();
	}

	List<TypeReference<InterfaceDecl>> getSuperInterfaces() {
		return interfaceVisitors.stream()
			.map(TypeVisitor::getType)
			.toList();
	}

	List<ITypeReference> getThrownExceptions() {
		return thrownVisitors.stream()
			.map(TypeVisitor::getType)
			.toList();
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		formalTypeParameterVisitors.putLast(name, new ArrayList<>());
	}

	@Override
	public SignatureVisitor visitClassBound() {
		var v = new TypeVisitor<>(api, typeRefFactory);
		formalTypeParameterVisitors.lastEntry().getValue().add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		var v = new TypeVisitor<>(api, typeRefFactory);
		formalTypeParameterVisitors.lastEntry().getValue().add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		var v = new TypeVisitor<TypeReference<ClassDecl>>(api, typeRefFactory);
		superClassVisitor = v;
		return v;
	}

	@Override
	public SignatureVisitor visitInterface() {
		var v = new TypeVisitor<TypeReference<InterfaceDecl>>(api, typeRefFactory);
		interfaceVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		var v = new TypeVisitor<>(api, typeRefFactory);
		parameterVisitors.add(v);
		return v;
	}

	@Override
	public SignatureVisitor visitReturnType() {
		var v = new TypeVisitor<>(api, typeRefFactory);
		returnTypeVisitor = v;
		return v;
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		var v = new TypeVisitor<>(api, typeRefFactory);
		thrownVisitors.add(v);
		return v;
	}

	public static class TypeVisitor<T extends ITypeReference> extends SignatureVisitor {
		TypeReferenceFactory factory;
		ITypeReference type;
		List<Supplier<ITypeReference>> visitors = new ArrayList<>();
		char wildcard = INSTANCEOF;

		TypeVisitor(int api, TypeReferenceFactory factory) {
			super(api);
			this.factory = factory;
		}

		TypeVisitor(int api, TypeReferenceFactory factory, char wildcard) {
			this(api, factory);
			this.wildcard = wildcard;
		}

		@Override
		public void visitTypeVariable(String name) {
			type = switch (wildcard) {
				case INSTANCEOF -> factory.createTypeParameterReference(name);
				case SUPER -> factory.createWildcardTypeReference(
					List.of(factory.createTypeParameterReference(name)), false);
				case EXTENDS -> factory.createWildcardTypeReference(
					List.of(factory.createTypeParameterReference(name)), true);
				default -> throw new IllegalStateException("ASM is drunk");
			};
		}

		@Override
		public void visitClassType(String name) {
			String typeName = name.replace('/', '.');
			type = switch (wildcard) {
				case INSTANCEOF -> factory.createTypeReference(typeName);
				case SUPER -> factory.createWildcardTypeReference(
					List.of(factory.createTypeReference(typeName)), false);
				case EXTENDS -> factory.createWildcardTypeReference(
					List.of(factory.createTypeReference(typeName)), true);
				default -> throw new IllegalStateException("ASM is drunk");
			};
		}

		@Override
		public SignatureVisitor visitArrayType() {
			TypeVisitor<ITypeReference> visitor = new TypeVisitor<>(api, factory);
			visitors.add(() -> {
				// If we've got an array, just increment the dimension
				ITypeReference arrayType = visitor.getType() instanceof ArrayTypeReference atr
					? factory.createArrayTypeReference(atr.componentType(), atr.dimension() + 1)
					: factory.createArrayTypeReference(visitor.getType(), 1);

				// If we're currently building a wildcard, we must wrap it
				return switch (wildcard) {
					case INSTANCEOF -> arrayType;
					case EXTENDS -> factory.createWildcardTypeReference(List.of(arrayType), true);
					case SUPER -> factory.createWildcardTypeReference(List.of(arrayType), false);
					default -> throw new IllegalStateException("ASM is drunk");
				};
			});
			return visitor;
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
				default ->  factory.createPrimitiveTypeReference("double");
			};
		}

		@Override
		public void visitInnerClassType(String name) {
			// Discard what we had already (a visitClassType with possible type args that are irrelevant)
			// and just register the inner, most precise type
			visitors.clear();
			visitClassType(String.format("%s$%s", type.getQualifiedName(), name));
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			TypeVisitor<ITypeReference> visitor = new TypeVisitor<>(api, factory, wildcard);
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
					var newBounds = visitors.stream()
						.map(Supplier::get)
						.toList();
					// FIXME: this getLast() won't last
					type = factory.createWildcardTypeReference(
						List.of(factory.createTypeReference(wtr.bounds().getLast().getQualifiedName(),
							newBounds)),
						wildcard == EXTENDS);
				} else {
					// We attach the collected bounds to the current type
					type = factory.createTypeReference(type.getQualifiedName(), visitors.stream().map(Supplier::get).toList());
				}
			}
		}

		public T getType() {
			return (T) (type != null ? type : visitors.getFirst().get()); // prayge
		}
	}
}
