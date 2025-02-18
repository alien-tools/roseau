package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.result.ToolResult;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.*;
import japicmp.output.Filter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JapicmpTool extends AbstractTool {
	public JapicmpTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		long startTime = System.currentTimeMillis();

		Options opts = Options.newDefault();
		opts.setAccessModifier(AccessModifier.PACKAGE_PROTECTED);
		opts.setOutputOnlyModifications(true);
		opts.setIgnoreMissingClasses(true);
		opts.setIncludeSynthetic(true);
		var comparatorOptions = JarArchiveComparatorOptions.of(opts);
		var jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

		var v1Archive = new JApiCmpArchive(v1Path.toFile(), "1.0.0");
		var v2Archive = new JApiCmpArchive(v2Path.toFile(), "2.0.0");

		List<JApiClass> jApiClasses = jarArchiveComparator.compare(v1Archive, v2Archive);
		List<JApiCompatibilityChange> allBreakingChanges = new ArrayList<>();
		Filter.filter(jApiClasses, new Filter.FilterVisitor() {
			@Override public void visit(Iterator<JApiClass> iterator, JApiClass jApiClass) {
				allBreakingChanges.addAll(jApiClass.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiMethod> iterator, JApiMethod jApiMethod) {
				allBreakingChanges.addAll(jApiMethod.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiConstructor> iterator, JApiConstructor jApiConstructor) {
				allBreakingChanges.addAll(jApiConstructor.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiImplementedInterface> iterator, JApiImplementedInterface jApiImplementedInterface) {
				allBreakingChanges.addAll(jApiImplementedInterface.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiField> iterator, JApiField jApiField) {
				allBreakingChanges.addAll(jApiField.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiAnnotation> iterator, JApiAnnotation jApiAnnotation) {
				allBreakingChanges.addAll(jApiAnnotation.getCompatibilityChanges());
			}
			@Override public void visit(JApiSuperclass jApiSuperclass) {
				allBreakingChanges.addAll(jApiSuperclass.getCompatibilityChanges());
			}
		});
		var realBreakingClasses = allBreakingChanges.stream()
				.filter(bc -> !bc.isBinaryCompatible() || !bc.isSourceCompatible())
				.toList();
		var isBreaking = !realBreakingClasses.isEmpty();

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("japicmp", executionTime, isBreaking);
	}
}
