package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.AnnotationDecl;
import io.github.alien.roseau.api.model.AnnotationMethodDecl;
import io.github.alien.roseau.api.model.ClassDecl;
import io.github.alien.roseau.api.model.EnumDecl;
import io.github.alien.roseau.api.model.InterfaceDecl;
import io.github.alien.roseau.api.model.RecordDecl;
import io.github.alien.roseau.api.model.TypeDecl;

public final class ApiWalker {
	private final API v1;
	private final API v2;
	private final SymbolMatcher matcher;

	public ApiWalker(API v1, API v2, SymbolMatcher matcher) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(matcher);
		this.v1 = v1;
		this.v2 = v2;
		this.matcher = matcher;
	}

	public <T> T walk(ApiDiffer<T> sink) {
		Preconditions.checkNotNull(sink);
		v1.getExportedTypes().parallelStream().forEach(t1 -> {
			matcher.matchType(v2, t1).ifPresentOrElse(
				t2 -> {
					onMatchedType(sink, t1, t2);
					walkMembers(t1, t2, sink);
				},
				() -> onRemovedType(sink, t1)
			);
		});

		v2.getExportedTypes().parallelStream()
			.filter(t2 -> matcher.matchType(v1, t2).isEmpty())
			.forEach(t2 -> onAddedType(sink, t2));

		return sink.get();
	}

	private <T> void walkMembers(TypeDecl t1, TypeDecl t2, ApiDiffer<T> sink) {
		v1.getExportedFields(t1).forEach(f1 ->
			matcher.matchField(v2, t2, f1).ifPresentOrElse(
				f2 -> sink.onMatchedField(t1, t2, f1, f2),
				() -> sink.onRemovedField(t1, f1)
			)
		);

		v2.getExportedFields(t2).stream()
			.filter(f2 -> matcher.matchField(v1, t1, f2).isEmpty())
			.forEach(f2 -> sink.onAddedField(t2, f2));

		v1.getExportedMethods(t1).forEach(m1 ->
			matcher.matchMethod(v2, t2, m1).ifPresentOrElse(
				m2 -> sink.onMatchedMethod(t1, t2, m1, m2),
				() -> sink.onRemovedMethod(t1, m1)
			)
		);

		v2.getExportedMethods(t2).stream()
			.filter(m2 -> matcher.matchMethod(v1, t1, m2).isEmpty())
			.forEach(m2 -> sink.onAddedMethod(t2, m2));

		if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2) {
			c1.getDeclaredConstructors().forEach(cons1 ->
				matcher.matchConstructor(v2, c2, cons1).ifPresentOrElse(
					cons2 -> sink.onMatchedConstructor(c1, c2, cons1, cons2),
					() -> sink.onRemovedConstructor(c1, cons1)
				)
			);

			c2.getDeclaredConstructors().stream()
				.filter(cons2 -> matcher.matchConstructor(v1, c1, cons2).isEmpty())
				.forEach(cons2 -> sink.onAddedConstructor(c2, cons2));
		}

		if (t1 instanceof AnnotationDecl a1 && t2 instanceof AnnotationDecl a2) {
			a1.getAnnotationMethods().forEach(m1 ->
				matcher.matchMethod(v2, a2, m1)
					.filter(AnnotationMethodDecl.class::isInstance)
					.map(AnnotationMethodDecl.class::cast)
					.ifPresentOrElse(
						m2 -> sink.onMatchedAnnotationMethod(a1, a2, m1, m2),
						() -> sink.onRemovedAnnotationMethod(a1, m1)
				)
			);

			a2.getAnnotationMethods().stream()
				.filter(m2 -> matcher.matchMethod(v1, a1, m2).isEmpty())
				.forEach(m2 -> sink.onAddedAnnotationMethod(a2, m2));
		}
	}

	private static <T> void onMatchedType(ApiDiffer<T> sink, TypeDecl t1, TypeDecl t2) {
		sink.onMatchedType(t1, t2);
		switch (t1) {
			case RecordDecl r1     when t2 instanceof RecordDecl r2     -> sink.onMatchedRecord(r1, r2);
			case EnumDecl e1       when t2 instanceof EnumDecl e2       -> sink.onMatchedEnum(e1, e2);
			case AnnotationDecl a1 when t2 instanceof AnnotationDecl a2 -> sink.onMatchedAnnotation(a1, a2);
			case InterfaceDecl i1  when t2 instanceof InterfaceDecl i2  -> sink.onMatchedInterface(i1, i2);
			case ClassDecl c1      when t2 instanceof ClassDecl c2      -> sink.onMatchedClass(c1, c2);
			default -> {}
		}
	}

	private static <T> void onRemovedType(ApiDiffer<T> sink, TypeDecl type) {
		sink.onRemovedType(type);
		switch (type) {
			case RecordDecl r     -> sink.onRemovedRecord(r);
			case EnumDecl e       -> sink.onRemovedEnum(e);
			case AnnotationDecl a -> sink.onRemovedAnnotation(a);
			case InterfaceDecl i  -> sink.onRemovedInterface(i);
			case ClassDecl c      -> sink.onRemovedClass(c);
		}
	}

	private static <T> void onAddedType(ApiDiffer<T> sink, TypeDecl type) {
		sink.onAddedType(type);
		switch (type) {
			case RecordDecl r     -> sink.onAddedRecord(r);
			case EnumDecl e       -> sink.onAddedEnum(e);
			case AnnotationDecl a -> sink.onAddedAnnotation(a);
			case InterfaceDecl i  -> sink.onAddedInterface(i);
			case ClassDecl c      -> sink.onAddedClass(c);
		}
	}
}
