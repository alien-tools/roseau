package io.github.alien.roseau.extractors.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AsmAnnotationVisitor extends AnnotationVisitor {
	private final String annotationDescriptor;
	private final Data annotation;
	private final Set<ElementType> targets;

	record Data(String descriptor, Map<String, String> values) {
		Data(String descriptor) {
			this(descriptor, new HashMap<>());
		}
	}

	AsmAnnotationVisitor(int api, String annotationDescriptor, Data annotation, Set<ElementType> targets) {
		super(api);
		this.annotationDescriptor = annotationDescriptor;
		this.annotation = annotation;
		this.targets = targets;
	}

	AsmAnnotationVisitor(int api, String annotationDescriptor, Data annotation) {
		this(api, annotationDescriptor, annotation, new HashSet<>());
	}

	@Override
	public void visit(String name, Object value) {
		annotation.values().put(name, formatAnnotationValue(value));
	}

	@Override
	public void visitEnum(String name, String descriptor, String value) {
		annotation.values().put(name, descriptorToFqn(descriptor) + "." + value);
		if ("Ljava/lang/annotation/ElementType;".equals(descriptor)) {
			targets.add(ElementType.valueOf(value));
		}
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		annotation.values().put(name, "{}");
		if ("value".equals(name) && "Ljava/lang/annotation/Target;".equals(annotationDescriptor)) {
			return new AnnotationVisitor(api) {
				@Override
				public void visitEnum(String name, String descriptor, String value) {
					if ("Ljava/lang/annotation/ElementType;".equals(descriptor)) {
						targets.add(ElementType.valueOf(value));
					}
				}
			};
		}
		return null;
	}

	private static String formatAnnotationValue(Object value) {
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
