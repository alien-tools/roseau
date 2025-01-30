package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import com.github.maracas.roseau.combinatorial.v2.benchmark.ToolResult;
import japicmp.cmp.JApiCmpArchive;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.model.JApiClass;

import java.nio.file.Path;
import java.util.List;

public final class JapicmpTool extends AbstractTool {
	public JapicmpTool(Path v1Path, Path v2Path) {
		super(v1Path, v2Path);
	}

	@Override
	public ToolResult detectBreakingChanges() {
		var comparatorOptions = new JarArchiveComparatorOptions();
		var jarArchiveComparator = new JarArchiveComparator(comparatorOptions);

		var v1Archive = new JApiCmpArchive(v1Path.toFile(), "1.0.0");
		var v2Archive = new JApiCmpArchive(v2Path.toFile(), "2.0.0");

		long startTime = System.currentTimeMillis();
		List<JApiClass> jApiClasses = jarArchiveComparator.compare(v1Archive, v2Archive);
		long executionTime = System.currentTimeMillis() - startTime;

		return new ToolResult(executionTime, !jApiClasses.isEmpty());
	}
}
