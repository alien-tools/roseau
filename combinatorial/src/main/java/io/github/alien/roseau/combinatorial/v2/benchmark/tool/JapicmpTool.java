package io.github.alien.roseau.combinatorial.v2.benchmark.tool;

import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiClass;
import japicmp.model.JApiCompatibilityChange;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
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
		List<JApiCompatibilityChange> breakingChanges = new ArrayList<>();
		Filter.filter(jApiClasses, new Filter.FilterVisitor() {
			@Override public void visit(Iterator<JApiClass> iterator, JApiClass jApiClass) {
				breakingChanges.addAll(jApiClass.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiMethod> iterator, JApiMethod jApiMethod) {
				breakingChanges.addAll(jApiMethod.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiConstructor> iterator, JApiConstructor jApiConstructor) {
				breakingChanges.addAll(jApiConstructor.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiImplementedInterface> iterator, JApiImplementedInterface jApiImplementedInterface) { breakingChanges.addAll(jApiImplementedInterface.getCompatibilityChanges()); }
			@Override public void visit(Iterator<JApiField> iterator, JApiField jApiField) {
				breakingChanges.addAll(jApiField.getCompatibilityChanges());
			}
			@Override public void visit(Iterator<JApiAnnotation> iterator, JApiAnnotation jApiAnnotation) {
				breakingChanges.addAll(jApiAnnotation.getCompatibilityChanges());
			}
			@Override public void visit(JApiSuperclass jApiSuperclass) {
				breakingChanges.addAll(jApiSuperclass.getCompatibilityChanges());
			}
		});

		var isBinaryCompatible = breakingChanges.stream().allMatch(JApiCompatibilityChange::isBinaryCompatible);
		var isSourceCompatible = breakingChanges.stream().allMatch(JApiCompatibilityChange::isSourceCompatible);

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("Japicmp", executionTime, !isBinaryCompatible, !isSourceCompatible);
	}
}
