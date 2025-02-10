package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.result.ToolResult;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiClass;

import java.nio.file.Path;
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
		var realBreakingClasses = jApiClasses.stream()
				.filter(cls -> !cls.isBinaryCompatible() || !cls.isSourceCompatible())
				.toList();
		var isBreaking = !realBreakingClasses.isEmpty();

		long executionTime = System.currentTimeMillis() - startTime;
		return new ToolResult("japicmp", executionTime, isBreaking);
	}
}
