package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;
import com.github.maracas.roseau.combinatorial.v2.breaker.intf.RemoveInterfaceStrategy;

public class BreakingChangesGeneratorVisitor extends AbstractAPIVisitor {
	private final API apiV1;
	private final NewApiQueue queue;

	public BreakingChangesGeneratorVisitor(API apiV1, NewApiQueue queue) {
		this.apiV1 = apiV1;
		this.queue = queue;
	}

	public Visit symbol(Symbol it) {
		switch (it) {
			case EnumDecl e: breakEnumDecl(e); break;
			case RecordDecl r: breakRecordDecl(r); break;
			case ClassDecl c: breakClassDecl(c); break;
			case InterfaceDecl i: breakInterfaceDecl(i); break;
			case ConstructorDecl c: breakConstructorDecl(c); break;
			case FieldDecl f: breakFieldDecl(f); break;
			case MethodDecl m: breakMethodDecl(m); break;
			case AnnotationDecl ignored: break;
		}

		return () -> it.getAnnotations().forEach(ann -> $(ann).visit());
	}

	private void breakEnumDecl(EnumDecl e) {
		// Do something with the enum
	}

	private void breakRecordDecl(RecordDecl r) {
		// Do something with the record
	}

	private void breakClassDecl(ClassDecl c) {
		// Do something with the class
	}

	private void breakInterfaceDecl(InterfaceDecl i) {
		new RemoveInterfaceStrategy(i, queue).breakApi(apiV1);
	}

	private void breakConstructorDecl(ConstructorDecl c) {
		// Do something with the constructor
	}

	private void breakFieldDecl(FieldDecl f) {
		// Do something with the field
	}

	private void breakMethodDecl(MethodDecl m) {
		// Do something with the method
	}
}
