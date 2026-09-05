package io.github.alien.roseau.extractors.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

class AsmAnnotationVisitor extends AnnotationVisitor {
	private final String annotationDescriptor;
	private final BiConsumer<String, String> values;
	private final Set<ElementType> targets;

	record Data(String descriptor, Map<String, String> values) {
		Data(String descriptor) {
			this(descriptor, new HashMap<>());
		}
	}

	AsmAnnotationVisitor(int api, String annotationDescriptor, Data annotation, Set<ElementType> targets) {
		this(api, annotationDescriptor, annotation.values()::put, targets);
	}

	private AsmAnnotationVisitor(int api, String annotationDescriptor, BiConsumer<String, String> values,
	                             Set<ElementType> targets) {
		super(api);
		this.annotationDescriptor = annotationDescriptor;
		this.values = values;
		this.targets = targets;
	}

	AsmAnnotationVisitor(int api, String annotationDescriptor, Data annotation) {
		this(api, annotationDescriptor, annotation, new HashSet<>());
	}

	@Override
	public void visit(String name, Object value) {
		values.accept(name, formatAnnotationValue(value));
	}

	@Override
	public void visitEnum(String name, String descriptor, String value) {
		values.accept(name, descriptorToFqn(descriptor) + "." + value);
		if ("Ljava/lang/annotation/Target;".equals(annotationDescriptor)
			&& "Ljava/lang/annotation/ElementType;".equals(descriptor)) {
			targets.add(ElementType.valueOf(value));
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String descriptor) {
		Data nested = new Data(descriptor);
		return new AsmAnnotationVisitor(api, descriptor, nested) {
			@Override
			public void visitEnd() {
				values.accept(name, nested.values().entrySet().stream()
					.sorted(Map.Entry.comparingByKey())
					.map(entry -> entry.getKey() + "=" + entry.getValue())
					.collect(Collectors.joining(", ", "@" + descriptorToFqn(descriptor) + "(", ")")));
			}
		};
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		StringJoiner elements = new StringJoiner(", ", "{", "}");
		// Array elements use the same callbacks as annotation members, with unnamed values.
		return new AsmAnnotationVisitor(api, annotationDescriptor, (_, value) -> elements.add(value), targets) {
			@Override
			public void visitEnd() {
				values.accept(name, elements.toString());
			}
		};
	}

	private static String formatAnnotationValue(Object value) {
		// ASM may deliver primitive arrays directly through visit.
		if (value.getClass().isArray()) {
			StringJoiner values = new StringJoiner(", ", "{", "}");
			for (int i = 0; i < java.lang.reflect.Array.getLength(value); i++) {
				values.add(formatAnnotationValue(java.lang.reflect.Array.get(value, i)));
			}
			return values.toString();
		}
		// Class<?> value
		if (value instanceof Type type) {
			return descriptorToFqn(type.toString());
		}
		return value.toString();
	}

	private static String descriptorToFqn(String descriptor) {
		return Type.getType(descriptor).getClassName();
	}
}
