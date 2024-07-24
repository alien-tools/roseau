package com.github.maracas.roseau.differential;

import com.github.maracas.roseau.api.SpoonAPIExtractor;
import com.github.maracas.roseau.diff.APIDiff;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import japicmp.output.Filter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class JapicmpTest {
	Path jarV1 = Path.of("/home/dig/repositories/japicmp/japicmp-testbase/" +
		"japicmp-test-v1/target/japicmp-test-v1-0.21.3-SNAPSHOT.jar");
	Path jarV2 = Path.of("/home/dig/repositories/japicmp/japicmp-testbase/" +
		"japicmp-test-v2/target/japicmp-test-v2-0.21.3-SNAPSHOT.jar");
	Path sourcesV1 = Path.of("/home/dig/repositories/japicmp/japicmp-testbase/japicmp-test-v1/");
	Path sourcesV2 = Path.of("/home/dig/repositories/japicmp/japicmp-testbase/japicmp-test-v2/");

	record BC(String qualifiedName, String change) {}

	List<BC> buildJapicmp() {
		var comparatorOptions = new JarArchiveComparatorOptions();
		comparatorOptions.getIgnoreMissingClasses().setIgnoreAllMissingClasses(true);
		var jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

		var classes = jarArchiveComparator.compare(
			new JApiCmpArchive(jarV1.toFile(), "v1"),
			new JApiCmpArchive(jarV2.toFile(), "v2")
		);

		var bcs = new ArrayList<BC>();
		Filter.filter(
			classes.stream()
				.filter(cls -> !(cls.isSourceCompatible() && cls.isBinaryCompatible()) && cls.getChangeStatus() != JApiChangeStatus.UNCHANGED)
				.toList(),
			new Filter.FilterVisitor() {
			@Override
			public void visit(Iterator<JApiClass> iterator, JApiClass jApiClass) {
				jApiClass.getCompatibilityChanges().forEach(ch -> bcs.add(
					new BC(jApiClass.getFullyQualifiedName(), ch.getType().name())));
			}

			@Override
			public void visit(Iterator<JApiMethod> iterator, JApiMethod jApiMethod) {
				jApiMethod.getCompatibilityChanges().forEach(ch -> bcs.add(
					new BC(jApiMethod.getjApiClass().getFullyQualifiedName() + "." + jApiMethod.getName(), ch.getType().name())));
			}

			@Override
			public void visit(Iterator<JApiConstructor> iterator, JApiConstructor jApiConstructor) {
				jApiConstructor.getCompatibilityChanges().forEach(ch -> bcs.add(
					new BC(jApiConstructor.getjApiClass().getFullyQualifiedName() + "." + jApiConstructor.getName(), ch.getType().name())));
			}

			@Override
			public void visit(Iterator<JApiImplementedInterface> iterator, JApiImplementedInterface jApiImplementedInterface) {
				jApiImplementedInterface.getCompatibilityChanges().forEach(ch -> bcs.add(new BC(jApiImplementedInterface.getFullyQualifiedName(), ch.getType().name())));
			}

			@Override
			public void visit(Iterator<JApiField> iterator, JApiField jApiField) {
				jApiField.getCompatibilityChanges().forEach(ch -> bcs.add(
					new BC(jApiField.getjApiClass().getFullyQualifiedName() + "." + jApiField.getName(), ch.getType().name())));
			}

			@Override
			public void visit(Iterator<JApiAnnotation> iterator, JApiAnnotation jApiAnnotation) {
				jApiAnnotation.getCompatibilityChanges().forEach(ch -> bcs.add(new BC(jApiAnnotation.getFullyQualifiedName(), ch.getType().name())));
			}

			@Override
			public void visit(JApiSuperclass jApiSuperclass) {
				jApiSuperclass.getCompatibilityChanges().forEach(ch -> bcs.add(new BC(jApiSuperclass.getJApiClassOwning().getFullyQualifiedName(), ch.getType().name())));
			}
		});

		return bcs;
	}

	List<BC> buildRoseau() {
		var apiExtractor = new SpoonAPIExtractor();
		var apiV1 = apiExtractor.extractAPI(sourcesV1);
		var apiV2 = apiExtractor.extractAPI(sourcesV2);
		return new APIDiff(apiV1, apiV2).diff().stream()
			.map(bc -> new BC(bc.impactedSymbol().getQualifiedName(), bc.kind().name()))
			.toList();
	}

	@Test
	void foo() {
		var japiDiff = buildJapicmp();
		var roseauDiff = buildRoseau();

		japiDiff.forEach(bc -> {
			if (bc.qualifiedName().endsWith("AnnotatedClass"))
				System.out.println("j="+bc);
		});

		roseauDiff.forEach(bc -> {
			if (bc.qualifiedName().endsWith("AnnotatedClass"))
				System.out.println("r="+bc);
		});

		System.out.printf("japi: %dBCs, roseau: %dBCs%n", japiDiff.size(), roseauDiff.size());

		compare(japiDiff, roseauDiff);

//		System.out.println("### Comparing japicmp vs roseau ###");
//		var japiExclusive = compare(japiDiff, roseauDiff);
//		System.out.println("### Comparing roseau vs japicmp ###");
//		var roseauExclusive = compare(roseauDiff, japiDiff);
//
//		System.out.println("### Exclusive to japicmp ###");
//		System.out.println(japiExclusive);
//
//		System.out.println("### Exclusive to roseau ###");
//		System.out.println(roseauExclusive);
//
//		System.out.printf("japicmp found %d/%d BCs not in roseau%n", japiExclusive.size(), japiDiff.size());
//		System.out.printf("Roseau found %d/%d BCs not in japicmp%n", roseauExclusive.size(), roseauDiff.size());
	}

	void compare(List<BC> l1, List<BC> l2) {
		Stream.concat(
			l1.stream().map(BC::qualifiedName),
			l2.stream().map(BC::qualifiedName)
		).distinct().forEach(symbol -> {
			System.out.println("For " + symbol);

			var symbolBC1 = l1.stream().filter(bc -> bc.qualifiedName().equals(symbol)).toList();
			var symbolBC2 = l2.stream().filter(bc -> bc.qualifiedName().equals(symbol)).toList();

			if (symbolBC1.isEmpty())
				System.out.println("\t❓ No BC in l1 (" + symbolBC2.stream().map(BC::change).collect(Collectors.joining("/")) + ")");
			else if (symbolBC2.isEmpty())
				System.out.println("\t❓ No BC in l2 (" + symbolBC1.stream().map(BC::change).collect(Collectors.joining("/")) + ")");
			else {
				symbolBC1.forEach(bc1 -> {
					var matches = symbolBC2.stream().filter(bc2 -> changesMatch(bc1.change(), bc2.change()) || changesMatch(bc2.change(), bc1.change())).toList();
					if (matches.isEmpty())
						System.out.println("\t❌ " + bc1.change() + " exclusive to l1");
					else
						System.out.println("\t✓ " + bc1.change() + " matched by " + matches.stream().map(BC::change).collect(Collectors.joining("/")));
				});

				symbolBC2.forEach(bc2 -> {
					var matches = symbolBC1.stream().filter(bc1 -> changesMatch(bc1.change(), bc2.change()) || changesMatch(bc2.change(), bc1.change())).toList();
					if (matches.isEmpty())
						System.out.println("\t❌ " + bc2.change() + " exclusive to l2");
					else
						System.out.println("\t✓ " + bc2.change() + " matched by " + matches.stream().map(BC::change).collect(Collectors.joining("/")));
				});
			}
		});
	}

	public boolean changesMatch(String c1, String c2) {
		return c1.equals(c2)
			|| c2.equals("TYPE_REMOVED") && (c1.equals("CLASS_REMOVED") || c1.equals("INTERFACE_REMOVED"))
			|| c2.equals("METHOD_REMOVED") && c1.equals("METHOD_LESS_ACCESSIBLE")
			|| c2.equals("FIELD_REMOVED") && c1.equals("FIELD_LESS_ACCESSIBLE")
			|| c2.equals("CONSTRUCTOR_REMOVED") && c1.equals("CONSTRUCTOR_LESS_ACCESSIBLE")
			|| c2.equals("METHOD_ABSTRACT_ADDED_TO_CLASS") && c1.equals("METHOD_ABSTRACT_ADDED_IN_SUPERCLASS")
			|| (c2.equals("TYPE_REMOVED") || c2.equals("TYPE_NOW_PROTECTED")) && (c1.equals("CLASS_NO_LONGER_PUBLIC") || c1.equals("CLASS_LESS_ACCESSIBLE"));
	}
}
