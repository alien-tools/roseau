package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.*;
import com.github.maracas.roseau.api.visit.AbstractAPIVisitor;
import com.github.maracas.roseau.api.visit.Visit;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.v2.breaker.intf.RemoveInterfaceStrategy;
import com.github.maracas.roseau.combinatorial.v2.queue.NewApiQueue;

import java.nio.file.Path;

public final class BreakingChangesGeneratorVisitor extends AbstractAPIVisitor {
	private final NewApiQueue queue;

	private final Path apiExportPath;

	public BreakingChangesGeneratorVisitor(NewApiQueue queue, Path outputPath) {
		this.queue = queue;

		this.apiExportPath = outputPath.resolve(Constants.API_JSON);
	}

	public Visit symbol(Symbol it) {
		switch (it) {
			case EnumDecl e: breakEnumDecl(e); break;
			case RecordDecl r: breakRecordDecl(r); break;
			case ClassDecl c: breakClassDecl(c); break;
			case InterfaceDecl i: breakInterfaceDecl(i); break;
			case ConstructorDecl c: breakConstructorDecl(c); break;
			case EnumValueDecl eV: breakEnumValueDecl(eV); break;
			case RecordComponentDecl rC: breakRecordComponentDecl(rC); break;
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
		new RemoveInterfaceStrategy(i, queue).breakApi(apiExportPath);
	}

	private void breakConstructorDecl(ConstructorDecl c) {
		// Do something with the constructor
	}

	private void breakEnumValueDecl(EnumValueDecl eV) {
		// Do something with the enum value
	}

	private void breakRecordComponentDecl(RecordComponentDecl rC) {
		// Do something with the record component
	}

	private void breakFieldDecl(FieldDecl f) {
		// Do something with the field
	}

	private void breakMethodDecl(MethodDecl m) {
		// Do something with the method
	}
}
