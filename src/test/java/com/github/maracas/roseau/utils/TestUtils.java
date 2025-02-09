package com.github.maracas.roseau.utils;

import com.github.maracas.roseau.api.extractors.sources.SpoonAPIExtractor;
import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.api.model.AnnotationDecl;
import com.github.maracas.roseau.api.model.ClassDecl;
import com.github.maracas.roseau.api.model.EnumDecl;
import com.github.maracas.roseau.api.model.FieldDecl;
import com.github.maracas.roseau.api.model.InterfaceDecl;
import com.github.maracas.roseau.api.model.MethodDecl;
import com.github.maracas.roseau.api.model.RecordDecl;
import com.github.maracas.roseau.api.model.TypeDecl;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import org.opentest4j.AssertionFailedError;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.support.compiler.VirtualFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TestUtils {
	private TestUtils() {
	}

	public static void assertBC(String symbol, BreakingChangeKind kind, int line, List<BreakingChange> bcs) {
		List<BreakingChange> matches = bcs.stream()
			.filter(bc ->
				   kind == bc.kind()
				&& line == bc.impactedSymbol().getLocation().line()
				&& symbol.equals(bc.impactedSymbol().getQualifiedName())
			).toList();

		if (matches.size() != 1) {
			String desc = "[%s, %s, %d]".formatted(symbol, kind, line);
			String found = bcs.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("No breaking change", desc, found);
		}
	}

	public static void assertNoBC(List<BreakingChange> bcs) {
		if (!bcs.isEmpty()) {
			String found = bcs.stream()
				.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
				.collect(Collectors.joining(", "));
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
		}
	}

	public static void assertNoBC(BreakingChangeKind kind, List<BreakingChange> bcs) {
		String found = bcs.stream()
			.filter(bc -> bc.kind() == kind)
			.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
			.collect(Collectors.joining(", "));

		if (!found.isEmpty())
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
	}

	public static void assertNoBC(int line, List<BreakingChange> bcs) {
		String found = bcs.stream()
			.filter(bc -> bc.impactedSymbol().getLocation().line() == line)
			.map(bc -> "[%s, %s, %d]".formatted(bc.impactedSymbol().getQualifiedName(), bc.kind(), bc.impactedSymbol().getLocation().line()))
			.collect(Collectors.joining(", "));

		if (!found.isEmpty())
			throw new AssertionFailedError("Unexpected breaking change", "No breaking change", found);
	}

	public static TypeDecl assertType(API api, String name, String kind) {
		Optional<TypeDecl> findType = api.findType(name);

		if (findType.isEmpty())
			throw new AssertionFailedError("No such type", kind + " " + name, "No such type");
		else {
			TypeDecl cls = findType.get();

			if ("class".equals(kind) && !cls.isClass())
				throw new AssertionFailedError("Wrong kind", "class " + name, cls.getClass().getSimpleName() + " " + name);
			if ("annotation".equals(kind) && !cls.isAnnotation())
				throw new AssertionFailedError("Wrong kind", "annotation " + name, cls.getClass().getSimpleName() + " " + name);
			if ("interface".equals(kind) && !cls.isInterface())
				throw new AssertionFailedError("Wrong kind", "interface " + name, cls.getClass().getSimpleName() + " " + name);
			if ("enum".equals(kind) && !cls.isEnum())
				throw new AssertionFailedError("Wrong kind", "enum " + name, cls.getClass().getSimpleName() + " " + name);
			if ("record".equals(kind) && !cls.isRecord())
				throw new AssertionFailedError("Wrong kind", "record " + name, cls.getClass().getSimpleName() + " " + name);

			return cls;
		}
	}

	public static void assertNoType(API api, String name) {
		Optional<TypeDecl> findType = api.findType(name);

		if (findType.isPresent())
			throw new AssertionFailedError("Unexpected type", "No such type", findType.get().getQualifiedName());
	}

	public static FieldDecl assertField(TypeDecl decl, String name) {
		Optional<FieldDecl> findField = decl.findField(name);

		if (findField.isEmpty())
			throw new AssertionFailedError("No such field", name, "No such field");
		else
			return findField.get();
	}

	public static void assertNoField(TypeDecl decl, String name) {
		Optional<FieldDecl> findField = decl.findField(name);

		if (findField.isPresent())
			throw new AssertionFailedError("Unexpected field", "No such field", findField.get().getQualifiedName());
	}

	public static MethodDecl assertMethod(TypeDecl decl, String erasure) {
		List<MethodDecl> findMethod = decl.getDeclaredMethods().stream()
			.filter(m -> m.getErasure().equals(erasure))
			.toList();

		if (findMethod.isEmpty())
			throw new AssertionFailedError("No such method", erasure, "No such method");
		return findMethod.getFirst();
	}

	public static ClassDecl assertClass(API api, String name) {
		return (ClassDecl) assertType(api, name, "class");
	}

	public static InterfaceDecl assertInterface(API api, String name) {
		return (InterfaceDecl) assertType(api, name, "interface");
	}

	public static RecordDecl assertRecord(API api, String name) {
		return (RecordDecl) assertType(api, name, "record");
	}

	public static EnumDecl assertEnum(API api, String name) {
		return (EnumDecl) assertType(api, name, "enum");
	}

	public static AnnotationDecl assertAnnotation(API api, String name) {
		return (AnnotationDecl) assertType(api, name, "annotation");
	}

	public static CtModel buildModel(String sources) {
		Launcher launcher = new Launcher();

		launcher.addInputResource(new VirtualFile(sources, "A.java"));
		launcher.getEnvironment().setComplianceLevel(17);
		launcher.getEnvironment().setLevel("TRACE");

		return launcher.buildModel();
	}

	public static API buildAPI(String sources) {
		CtModel m = buildModel(sources);
		SpoonAPIExtractor extractor = new SpoonAPIExtractor();
		return extractor.extractAPI(m);
	}

	public static List<BreakingChange> buildDiff(String sourcesV1, String sourcesV2) {
		APIDiff apiDiff = new APIDiff(buildAPI(sourcesV1), buildAPI(sourcesV2));
		apiDiff.diff();
		return apiDiff.getBreakingChanges();
	}
}
