package com.github.maracas.roseau.differential;

/*import com.github.maracas.roseau.api.extractors.sources.SpoonAPIExtractor;
import com.github.maracas.roseau.diff.APIDiff;
import com.github.maracas.roseau.diff.changes.BreakingChange;
import com.github.maracas.roseau.diff.changes.BreakingChangeKind;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.AccessModifier;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibilityChange;
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

class JapicmpTest {
	Path jarV1 = Path.of("/home/dig/repositories/japicmp/japicmp-testbase/" +
		"japicmp-test-v1/target/japicmp-test-v1-0.20.1-SNAPSHOT.jar");
	Path jarV2 = Path.of("/home/dig/repositories/japicmp/japicmp-testbase/" +
		"japicmp-test-v2/target/japicmp-test-v2-0.20.1-SNAPSHOT.jar");
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
				jApiSuperclass.getCompatibilityChanges().forEach(ch -> bcs.add(new BC(jApiSuperclass.getOldSuperclassName().get(), ch.getType().name())));
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

		System.out.printf("japi: %dBCs, roseau: %dBCs%n", japiDiff.size(), roseauDiff.size());

		System.out.println("### Comparing japicmp vs roseau ###");
		var japiExclusive = compare(japiDiff, roseauDiff);
		System.out.println("### Comparing roseau vs japicmp ###");
		var roseauExclusive = compare(roseauDiff, japiDiff);

		System.out.println("### Exclusive to japicmp ###");
		System.out.println(japiExclusive);

		System.out.println("### Exclusive to roseau ###");
		System.out.println(roseauExclusive);

		System.out.printf("japicmp found %d/%d BCs not in roseau%n", japiExclusive.size(), japiDiff.size());
		System.out.printf("Roseau found %d/%d BCs not in japicmp%n", roseauExclusive.size(), roseauDiff.size());
	}

	List<BC> compare(List<BC> l1, List<BC> l2) {
		var excl = new ArrayList<BC>();

		l1.forEach(bc1 -> {
			var match = l2.stream()
				.filter(bc2 -> bc1.qualifiedName().equals(bc2.qualifiedName()))
				.findFirst();

			match.ifPresentOrElse(bc2 -> {
				System.out.println("✓ Found matching symbol " + bc2);
				if (bc2.change().equals(bc1.change())) {
					System.out.println("\t✓ " + bc2.change() + " matches");
				} else {
					System.out.println("\t❌ " + bc2.change() + " doesn't match " + bc1.change());
					excl.add(bc1);
				}
			}, () -> {
				System.out.println("❌ Not found " + bc1);
				excl.add(bc1);
			});
		});

		return excl;
	}
}*/
