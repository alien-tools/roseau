package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ClassDecl;
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

	public void walk(ApiDiffer sink) {
		v1.getExportedTypes().parallelStream().forEach(t1 -> {
			matcher.matchType(v2, t1).ifPresentOrElse(
				t2 -> {
					sink.onMatchedType(t1, t2);
					walkMembers(t1, t2, sink);
				},
				() -> sink.onRemovedType(t1)
			);
		});

		v2.getExportedTypes().parallelStream()
			.filter(t2 -> matcher.matchType(v1, t2).isEmpty())
			.forEach(sink::onAddedType);
	}

	private void walkMembers(TypeDecl t1, TypeDecl t2, ApiDiffer sink) {
		v1.getExportedFields(t1).forEach(f1 ->
			matcher.matchField(v2, t2, f1).ifPresentOrElse(
				f2 -> sink.onMatchedField(t1, f1, f2),
				() -> sink.onRemovedField(t1, f1)
			)
		);

		v2.getExportedFields(t2).stream()
			.filter(f2 -> matcher.matchField(v1, t2, f2).isEmpty())
			.forEach(f2 -> sink.onAddedField(t2, f2));

		v1.getExportedMethods(t1).forEach(m1 ->
			matcher.matchMethod(v2, t2, m1).ifPresentOrElse(
				m2 -> sink.onMatchedMethod(t1, m1, m2),
				() -> sink.onRemovedMethod(t1, m1)
			)
		);

		v2.getExportedMethods(t2).stream()
			.filter(m2 -> matcher.matchMethod(v1, t2, m2).isEmpty())
			.forEach(m2 -> sink.onAddedMethod(t2, m2));

		if (t1 instanceof ClassDecl c1 && t2 instanceof ClassDecl c2) {
			c1.getDeclaredConstructors().forEach(cons1 ->
				matcher.matchConstructor(v2, c2, cons1).ifPresentOrElse(
					cons2 -> sink.onMatchedConstructor(c1, cons1, cons2),
					() -> sink.onRemovedConstructor(c1, cons1)
				)
			);

			c2.getDeclaredConstructors().stream()
				.filter(cons2 -> c1.getDeclaredConstructors().stream().noneMatch(cons1 -> v1.haveSameErasure(cons1, cons2)))
				.forEach(cons2 -> sink.onAddedConstructor(c2, cons2));
		}
	}
}
